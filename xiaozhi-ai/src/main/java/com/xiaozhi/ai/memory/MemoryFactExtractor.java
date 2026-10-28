package com.xiaozhi.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.ai.llm.JsonMode;
import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.message.service.MessageService;
import com.xiaozhi.memoryfact.service.MemoryFactService;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.utils.JsonUtil;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 从最近一段对话里提炼值得长期记住的事实。
 * <p>
 * 只做「抽取 + 去重 + 落库」三件事：判断交给模型，落库前用文本相似度兜底，
 * 避免同一句话每次对话都被记一遍。
 */
@Slf4j
@Component
public class MemoryFactExtractor {

    private static final int MAX_FACTS = 5;
    private static final int HISTORY_LIMIT = 30;

    private static final String INSTRUCTION = "你是家庭语音助手的记忆整理器。根据下面这段对话，提炼出「值得长期记住」的事实，最多 5 条。"
        + "只记稳定信息：称呼、偏好、习惯、日程、家庭成员关系、重要物品；不要记一次性的问答和闲聊。"
        + "每条事实用一句陈述句，20 字以内，主语明确（如「妈妈喜欢乌龙茶」）。"
        + "没有值得记的就返回空数组。只输出 JSON 数组，不要解释："
        + "[{\"content\":\"...\",\"memberName\":\"妈妈或null\",\"category\":\"偏好|日程|关系|其它\"}]"
        + "\n对话如下：\n";

    @Resource
    private ChatModelFactory chatModelFactory;

    @Resource
    private MessageService messageService;

    @Resource
    private RoleService roleService;

    @Resource
    private MemoryFactService memoryFactService;

    @Resource
    private com.xiaozhi.memorygraph.service.MemoryGraphService memoryGraphService;

    @Resource
    private MemoryGraphExtractor memoryGraphExtractor;

    public List<MemoryFactBO> extract(Integer userId, String deviceId, Integer roleId, String memberName) {
        if (userId == null || !StringUtils.hasText(deviceId) || roleId == null) {
            return List.of();
        }
        RoleBO role = roleService.getBO(roleId);
        if (role == null) {
            return List.of();
        }
        List<MessageBO> history = messageService.listHistory(deviceId, roleId, HISTORY_LIMIT);
        String transcript = buildTranscript(history);
        if (!StringUtils.hasText(transcript)) {
            return List.of();
        }
        try {
            ChatModel chatModel = chatModelFactory.getChatModel(role);
            String raw = JsonMode.call(chatModel, options ->
                chatModel.call(new Prompt(INSTRUCTION + transcript, options)).getResult().getOutput().getText());
            List<FactDraft> drafts = parse(raw);
            List<String> existing = memoryFactService.listForPrompt(userId, memberName, 50).stream()
                .map(MemoryFactBO::getContent)
                .toList();
            List<MemoryFactBO> saved = new ArrayList<>();
            for (FactDraft draft : drafts) {
                if (draft == null || !StringUtils.hasText(draft.content()) || isDuplicate(draft.content(), existing)) {
                    continue;
                }
                MemoryFactBO bo = new MemoryFactBO();
                bo.setUserId(userId);
                bo.setMemberName(StringUtils.hasText(draft.memberName()) ? draft.memberName() : memberName);
                bo.setContent(draft.content().trim());
                bo.setCategory(draft.category());
                bo.setSource(MemoryFactBO.SOURCE_AUTO);
                saved.add(memoryFactService.create(bo));
                indexGraph(bo);
                if (saved.size() >= MAX_FACTS) {
                    break;
                }
            }
            log.info("记忆抽取完成 userId={} 新增={}", userId, saved.size());
            return saved;
        } catch (RuntimeException e) {
            log.warn("记忆抽取失败，本轮跳过 userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 顺手把事实抽成图谱三元组；抽不出来或落库失败都不影响事实本身。
     */
    private void indexGraph(MemoryFactBO fact) {
        try {
            memoryGraphExtractor.extract(fact.getContent()).ifPresent(triple ->
                memoryGraphService.addRelationByNames(fact.getUserId(), triple.from(), triple.relation(),
                    triple.to(), "person", null));
        } catch (RuntimeException e) {
            log.debug("事实转图谱失败，忽略 content={}", fact.getContent(), e);
        }
    }

    /** 解析模型输出：容忍围栏与说明文字，字段缺失也接受 */
    static List<FactDraft> parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String json = extractJsonArray(raw);
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<FactDraft> drafts = JsonUtil.fromJson(json, new TypeReference<List<FactDraft>>() {});
            return drafts == null ? List.of() : drafts;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** 从模型输出里截出 JSON 数组，容忍前后的说明文字与代码围栏 */
    static String extractJsonArray(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : null;
    }

    /** 去重：归一化后互相包含即视为同一条 */
    static boolean isDuplicate(String candidate, List<String> existing) {
        String normalized = normalize(candidate);
        if (normalized.isEmpty()) {
            return true;
        }
        for (String item : existing) {
            String other = normalize(item);
            if (other.isEmpty()) {
                continue;
            }
            if (normalized.contains(other) || other.contains(normalized)) {
                return true;
            }
            // "妈妈喜欢乌龙茶" vs "妈妈喜欢喝乌龙茶"：差一两个字也算同一条
            if (similarity(normalized, other) >= 0.8d) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符级编辑距离相似度（1 - 距离/较长串长度），够用且不需要额外依赖。
     */
    static double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1d;
        }
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 1d;
        }
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return 1d - ((double) prev[b.length()] / max);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("[\\s，。！？,.!?、]", "").toLowerCase(Locale.ROOT);
    }

    private String buildTranscript(List<MessageBO> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MessageBO message : history) {
            if (message == null || !StringUtils.hasText(message.getMessage())) {
                continue;
            }
            String speaker = "assistant".equalsIgnoreCase(message.getSender()) ? "助手" : "用户";
            builder.append(speaker).append("：").append(message.getMessage().trim()).append('\n');
        }
        return builder.toString();
    }

    public record FactDraft(String content, String memberName, String category) {
    }
}