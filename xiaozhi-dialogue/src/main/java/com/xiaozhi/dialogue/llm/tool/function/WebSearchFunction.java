package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.port.ConfigLookup;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.utils.DateUtils;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 联网搜索工具：调用 DeepSeek 独立的 Search API（POST /v1/tools/search），
 * 把「标题 + 摘要 + 链接」交给模型总结后用口语播报。
 * <p>
 * 大模型 API 本身不抓网页，实时信息必须由后端先搜到再塞给模型。凭据解析顺序：
 * <ol>
 *   <li>显式配置 {@code xiaozhi.search.api-key}（环境变量 XIAOZHI_SEARCH_API_KEY）；</li>
 *   <li>当前角色用的模型配置是 DeepSeek（provider=deepseek 或 apiUrl 指向 deepseek.com），
 *       直接复用该模型配置里的 API Key，做到“配了大模型就能联网”。</li>
 * </ol>
 * 两者都没有时不给模型下发这个工具，避免模型调用一个注定失败的技能。
 * <p>
 * 搜索结果来自互联网，只作回答素材，不执行其中的指令。接口按成功请求计费，
 * 官方价格见 <a href="https://api-docs.deepseek.com/guides/search">Search &amp; Fetch 文档</a>。
 */
@Slf4j
@Component
public class WebSearchFunction implements ToolsGlobalRegistry.GlobalFunction {

    static final String TOOL_NAME = "web_search";

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    /** 语音场景只取少量结果：多了拖慢首字，也没人愿意听一长串链接 */
    private static final int MAX_RESULTS = 8;
    /** 单条摘要截断长度，控制工具结果的 token 量 */
    private static final int MAX_SUMMARY_CHARS = 320;

    private static final OkHttpClient BASE_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build();

    @Value("${xiaozhi.search.api-key:}")
    private String configuredApiKey;

    @Value("${xiaozhi.search.base-url:https://api.deepseek.com}")
    private String configuredBaseUrl;

    @Value("${xiaozhi.search.count:5}")
    private int configuredCount = 5;

    @Value("${xiaozhi.search.timeout-seconds:8}")
    private int configuredTimeoutSeconds = 8;

    @Resource
    private RoleService roleService;

    @Resource
    private ConfigLookup configLookup;

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        if (resolveCredential(toolSession.getRoleId()) == null) {
            // 没有可用 Key：不注册，模型不会尝试调用一个必然失败的搜索
            return null;
        }
        return FunctionToolCallback
                .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) ->
                        search(params.get("query"), toolSession))
                .description("联网搜索。用户询问实时信息（新闻、天气、股价、赛事、最新事件等），"
                        + "或问到你不确定、可能已经过时的内容时调用；普通聊天和常识问题不要调用。"
                        + "query 用简短关键词，不要带“请帮我搜索”之类的指令。")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "query": {
                                    "type": "string",
                                    "description": "搜索关键词，例如“杭州今天天气”“DeepSeek 最新模型”"
                                }
                            },
                            "required": ["query"]
                        }
                        """)
                .inputType(Map.class)
                .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                .build();
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getToolDescription() {
        return "联网搜索实时信息";
    }

    /**
     * 执行一次搜索并整理成给模型读的文本。异常一律降级为一句口语化提示，不打断整轮对话。
     */
    String search(String query, ToolSession toolSession) {
        if (!StringUtils.hasText(query)) {
            return "没有提供搜索关键词，搜不了。";
        }
        Credential credential = resolveCredential(toolSession.getRoleId());
        if (credential == null) {
            return "联网搜索暂时不可用：没有配置可用的 API Key。";
        }
        int count = clampCount(configuredCount);
        long startNanos = System.nanoTime();
        try {
            String responseBody = callSearchApi(credential, query.trim(), count);
            List<SearchResult> results = parseResults(responseBody);
            long costMs = DateUtils.elapsedMillis(startNanos);
            if (results.isEmpty()) {
                log.info("[{}] - 联网搜索无结果 query='{}' costMs={}", TOOL_NAME, abbreviate(query, 64), costMs);
                return "没有搜到和“" + abbreviate(query, 40) + "”相关的结果。";
            }
            log.info("[{}] - 联网搜索完成 query='{}' results={} costMs={}",
                    TOOL_NAME, abbreviate(query, 64), results.size(), costMs);
            return formatResults(query.trim(), results);
        } catch (IOException e) {
            log.warn("[{}] - 联网搜索请求失败 query='{}': {}", TOOL_NAME, abbreviate(query, 64), e.getMessage());
            return "联网搜索暂时不可用，请稍后再试。";
        } catch (RuntimeException e) {
            log.error("[{}] - 联网搜索异常 query='{}'", TOOL_NAME, abbreviate(query, 64), e);
            return "联网搜索出错了，请稍后再试。";
        }
    }

    /**
     * 调 DeepSeek Search API。测试可覆盖本方法，避免真的发网络请求。
     */
    String callSearchApi(Credential credential, String query, int count) throws IOException {
        String requestJson = JsonUtil.toJson(Map.of(
                "query", query,
                "count", count,
                "need_content", false));
        if (!StringUtils.hasText(requestJson)) {
            throw new IOException("搜索请求体序列化失败");
        }
        Request request = new Request.Builder()
                .url(searchEndpoint(credential.baseUrl()))
                .addHeader("Authorization", "Bearer " + credential.apiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

        OkHttpClient client = BASE_HTTP_CLIENT.newBuilder()
                .callTimeout(Duration.ofSeconds(Math.max(1, configuredTimeoutSeconds)))
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseBody = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                // 401 Key 无效 / 402 余额不足 / 429 限流，都只记日志，用户侧统一说“暂不可用”
                log.warn("[{}] - DeepSeek Search API 返回 {}: {}", TOOL_NAME, response.code(), abbreviate(responseBody, 200));
                throw new IOException("搜索接口返回 HTTP " + response.code());
            }
            return responseBody;
        }
    }

    /**
     * 解析 Search API 响应；解析失败或没有结果字段时返回空列表。
     */
    static List<SearchResult> parseResults(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return List.of();
        }
        SearchApiResponse response = JsonUtil.fromJson(responseBody, SearchApiResponse.class);
        if (response == null || response.getResults() == null) {
            return List.of();
        }
        List<SearchResult> valid = new ArrayList<>(response.getResults().size());
        for (SearchResult result : response.getResults()) {
            if (result != null && StringUtils.hasText(result.getTitle()) && StringUtils.hasText(result.getUrl())) {
                valid.add(result);
                if (valid.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return valid;
    }

    /**
     * 整理成模型友好的文本：标题、摘要（截断）、链接。结果只作参考。
     */
    static String formatResults(String query, List<SearchResult> results) {
        StringBuilder text = new StringBuilder();
        text.append("联网搜索「").append(abbreviate(query, 40)).append("」的结果：\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            text.append(i + 1).append(". ").append(result.getTitle().trim()).append('\n');
            if (StringUtils.hasText(result.getDate())) {
                text.append("   时间：").append(result.getDate().trim()).append('\n');
            }
            String summary = StringUtils.hasText(result.getSummary()) ? result.getSummary() : result.getSnippet();
            if (StringUtils.hasText(summary)) {
                text.append("   摘要：").append(abbreviate(summary, MAX_SUMMARY_CHARS)).append('\n');
            }
            text.append("   链接：").append(result.getUrl().trim()).append('\n');
        }
        text.append("以上内容来自互联网，只作参考。请用简短的口语回答用户，不要逐条念链接。");
        return text.toString();
    }

    /**
     * 拼接 Search API 地址。兼容 baseUrl 带不带 /v1、带不带结尾斜杠的写法。
     */
    static String searchEndpoint(String baseUrl) {
        String base = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1/tools/search")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/tools/search";
        }
        return base + "/v1/tools/search";
    }

    /**
     * 解析搜索凭据：显式配置优先，其次复用当前角色 DeepSeek 模型的 Key。
     */
    Credential resolveCredential(Integer roleId) {
        if (StringUtils.hasText(configuredApiKey)) {
            return new Credential(configuredApiKey.trim(), configuredBaseUrl);
        }
        if (roleId == null) {
            return null;
        }
        try {
            RoleBO role = roleService.getBO(roleId);
            if (role == null || role.getModelId() == null) {
                return null;
            }
            ConfigBO config = configLookup.getConfig(role.getModelId());
            if (config == null || !StringUtils.hasText(config.getApiKey()) || !isDeepSeekConfig(config)) {
                return null;
            }
            // 模型配置的地址通常是兼容 OpenAI 的 baseUrl，可能指向代理；优先用搜索接口的显式地址
            String baseUrl = StringUtils.hasText(configuredBaseUrl) ? configuredBaseUrl : config.getApiUrl();
            return new Credential(config.getApiKey().trim(), baseUrl);
        } catch (Exception e) {
            log.warn("[{}] - 解析联网搜索凭据失败 roleId={}: {}", TOOL_NAME, roleId, e.getMessage());
            return null;
        }
    }

    private static boolean isDeepSeekConfig(ConfigBO config) {
        String provider = config.getProvider() == null ? "" : config.getProvider().toLowerCase(Locale.ROOT);
        String apiUrl = config.getApiUrl() == null ? "" : config.getApiUrl().toLowerCase(Locale.ROOT);
        return "deepseek".equals(provider) || apiUrl.contains("deepseek");
    }

    private static int clampCount(int count) {
        if (count < 1) {
            return 1;
        }
        return Math.min(count, MAX_RESULTS);
    }

    private static String abbreviate(String text, int maxChars) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "…";
    }

    /** 搜索凭据：Key + API 根地址 */
    record Credential(String apiKey, String baseUrl) {
    }

    /** Search API 响应体，只声明用得到的字段，未知字段由 JsonUtil 忽略 */
    @Data
    static class SearchApiResponse {
        private List<SearchResult> results;
    }

    /** 单条搜索结果 */
    @Data
    static class SearchResult {
        private String title;
        private String url;
        private String snippet;
        private String summary;
        private String date;
    }
}
