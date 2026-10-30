package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.port.ConfigLookup;
import com.sun.net.httpserver.HttpServer;
import com.xiaozhi.role.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 联网搜索工具的纯单元测试：不发真实网络请求，HTTP 调用在用例里覆盖掉。
 */
class WebSearchFunctionTest {

    private WebSearchFunction function;
    private RoleService roleService;
    private ConfigLookup configLookup;

    @BeforeEach
    void setUp() {
        function = new WebSearchFunction();
        roleService = mock(RoleService.class);
        configLookup = mock(ConfigLookup.class);
        ReflectionTestUtils.setField(function, "roleService", roleService);
        ReflectionTestUtils.setField(function, "configLookup", configLookup);
        ReflectionTestUtils.setField(function, "configuredApiKey", "");
        ReflectionTestUtils.setField(function, "configuredBaseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(function, "configuredCount", 5);
        ReflectionTestUtils.setField(function, "configuredTimeoutSeconds", 8);
    }

    @Test
    void registersToolWhenDedicatedKeyConfigured() {
        ReflectionTestUtils.setField(function, "configuredApiKey", "sk-test");

        ToolCallback callback = function.getFunctionCallTool(toolSession(7));

        assertThat(callback).isNotNull();
        assertThat(callback.getToolDefinition().name()).isEqualTo(WebSearchFunction.TOOL_NAME);
        assertThat(callback.getToolDefinition().description()).contains("联网搜索");
    }

    @Test
    void hidesToolWhenNoKeyAndModelIsNotDeepSeek() {
        when(roleService.getBO(7)).thenReturn(role(7, 11));
        when(configLookup.getConfig(11)).thenReturn(config("openai", "https://api.openai.com", "sk-openai"));

        assertThat(function.getFunctionCallTool(toolSession(7))).isNull();
    }

    @Test
    void reusesDeepSeekChatModelKeyWhenDedicatedKeyMissing() {
        when(roleService.getBO(7)).thenReturn(role(7, 11));
        when(configLookup.getConfig(11)).thenReturn(config("deepseek", "https://api.deepseek.com", "sk-model"));

        assertThat(function.getFunctionCallTool(toolSession(7))).isNotNull();
        WebSearchFunction.Credential credential = function.resolveCredential(7);
        assertThat(credential).isNotNull();
        assertThat(credential.apiKey()).isEqualTo("sk-model");
        assertThat(credential.baseUrl()).isEqualTo("https://api.deepseek.com");
    }

    @Test
    void dedicatedKeyTakesPrecedenceOverModelConfig() {
        ReflectionTestUtils.setField(function, "configuredApiKey", "sk-search");

        WebSearchFunction.Credential credential = function.resolveCredential(7);

        assertThat(credential).isNotNull();
        assertThat(credential.apiKey()).isEqualTo("sk-search");
    }

    @Test
    void searchFormatsApiResponseForModel() {
        WebSearchFunction stub = new WebSearchFunction() {
            @Override
            String callSearchApi(WebSearchFunction.Credential credential, String query, int count) {
                return """
                        {"results":[
                          {"title":"DeepSeek V4 发布","url":"https://example.com/a",
                           "summary":"摘要一","date":"2026-06-01T00:00:00+00:00"},
                          {"title":"第二条","url":"https://example.com/b",
                           "snippet":"没有 summary 时回退 snippet"}
                        ]}
                        """;
            }
        };
        ReflectionTestUtils.setField(stub, "configuredApiKey", "sk-test");

        String result = stub.search("DeepSeek 最新模型", toolSession(7));

        assertThat(result).contains("联网搜索「DeepSeek 最新模型」的结果");
        assertThat(result).contains("1. DeepSeek V4 发布");
        assertThat(result).contains("摘要一");
        assertThat(result).contains("没有 summary 时回退 snippet");
        assertThat(result).contains("链接：https://example.com/a");
    }

    @Test
    void searchReturnsFriendlyTextWhenApiFails() {
        WebSearchFunction stub = new WebSearchFunction() {
            @Override
            String callSearchApi(WebSearchFunction.Credential credential, String query, int count) throws IOException {
                throw new IOException("HTTP 502");
            }
        };
        ReflectionTestUtils.setField(stub, "configuredApiKey", "sk-test");

        String result = stub.search("今天天气", toolSession(7));

        assertThat(result).contains("暂时不可用");
    }

    @Test
    void searchHandlesEmptyResultsAndBlankQuery() {
        WebSearchFunction stub = new WebSearchFunction() {
            @Override
            String callSearchApi(WebSearchFunction.Credential credential, String query, int count) {
                return "{\"results\":[]}";
            }
        };
        ReflectionTestUtils.setField(stub, "configuredApiKey", "sk-test");

        assertThat(stub.search("不存在的结果", toolSession(7))).contains("没有搜到");
        assertThat(stub.search("  ", toolSession(7))).contains("没有提供搜索关键词");
    }

    @Test
    void callsSearchApiOverHttpAndFormatsResponse() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestAuth = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/tools/search", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"results":[{"title":"本地标题","url":"https://example.com/x","summary":"本地摘要"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(function, "configuredApiKey", "sk-test");
            ReflectionTestUtils.setField(function, "configuredBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(function, "configuredCount", 3);

            String result = function.search("本地搜索", toolSession(7));

            assertThat(requestPath.get()).isEqualTo("/v1/tools/search");
            assertThat(requestAuth.get()).isEqualTo("Bearer sk-test");
            assertThat(requestBody.get()).contains("\"query\":\"本地搜索\"").contains("\"count\":3");
            assertThat(result).contains("1. 本地标题").contains("本地摘要").contains("https://example.com/x");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void searchReturnsFriendlyTextWhenApiRejects() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/tools/search", exchange -> {
            byte[] response = "{\"error\":{\"message\":\"insufficient balance\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(402, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(function, "configuredApiKey", "sk-test");
            ReflectionTestUtils.setField(function, "configuredBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            String result = function.search("余额不足测试", toolSession(7));

            assertThat(result).contains("暂时不可用");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parsesResultsAndSkipsInvalidItems() {
        List<WebSearchFunction.SearchResult> results = WebSearchFunction.parseResults("""
                {"results":[
                  {"title":"有效结果","url":"https://example.com/a"},
                  {"title":"","url":"https://example.com/b"},
                  {"url":"https://example.com/c"},
                  null
                ]}
                """);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("有效结果");
    }

    @Test
    void parseEmptyOrBrokenBodyReturnsEmptyList() {
        assertThat(WebSearchFunction.parseResults("")).isEmpty();
        assertThat(WebSearchFunction.parseResults(null)).isEmpty();
        assertThat(WebSearchFunction.parseResults("not-json")).isEmpty();
        assertThat(WebSearchFunction.parseResults("{\"results\":[]}")).isEmpty();
    }

    @Test
    void buildsEndpointForCommonBaseUrlForms() {
        assertThat(WebSearchFunction.searchEndpoint("https://api.deepseek.com"))
                .isEqualTo("https://api.deepseek.com/v1/tools/search");
        assertThat(WebSearchFunction.searchEndpoint("https://api.deepseek.com/"))
                .isEqualTo("https://api.deepseek.com/v1/tools/search");
        assertThat(WebSearchFunction.searchEndpoint("https://api.deepseek.com/v1"))
                .isEqualTo("https://api.deepseek.com/v1/tools/search");
        assertThat(WebSearchFunction.searchEndpoint("https://api.deepseek.com/v1/tools/search"))
                .isEqualTo("https://api.deepseek.com/v1/tools/search");
        assertThat(WebSearchFunction.searchEndpoint(null))
                .isEqualTo("https://api.deepseek.com/v1/tools/search");
    }

    @Test
    void truncatesLongSummaryToControlToolResultTokens() {
        WebSearchFunction.SearchResult result = new WebSearchFunction.SearchResult();
        result.setTitle("长摘要测试");
        result.setUrl("https://example.com/long");
        result.setSummary("长".repeat(1000));

        String formatted = WebSearchFunction.formatResults("测试", List.of(result));

        assertThat(formatted).contains("…");
        assertThat(formatted.length()).isLessThan(800);
    }

    @Test
    void toolSummaryIsPublishedForRoleToolList() {
        assertThat(function.getToolName()).isEqualTo(WebSearchFunction.TOOL_NAME);
        assertThat(function.getToolDescription()).contains("联网搜索");
    }

    private static ToolSession toolSession(Integer roleId) {
        ToolSession toolSession = mock(ToolSession.class);
        when(toolSession.getRoleId()).thenReturn(roleId);
        return toolSession;
    }

    private static RoleBO role(Integer roleId, Integer modelId) {
        RoleBO role = new RoleBO();
        role.setRoleId(roleId);
        role.setModelId(modelId);
        return role;
    }

    private static ConfigBO config(String provider, String apiUrl, String apiKey) {
        return new ConfigBO()
                .setProvider(provider)
                .setApiUrl(apiUrl)
                .setApiKey(apiKey);
    }
}
