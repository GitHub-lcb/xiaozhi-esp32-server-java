package com.xiaozhi.ai.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 工具召回：工具一多，全量塞给模型会挤占上下文、降低选择准确率。
 * <p>
 * 这里用「用户这句话与工具名/描述的字符重合度」做粗排，只保留最相关的前 N 个；
 * 工具总数不超过 N 时原样返回，不引入任何扰动。没有 embedding 依赖，纯本地、零成本。
 */
@Component
public class ToolSelector {

    @Value("${xiaozhi.tools.max-count:12}")
    private int maxCount = 12;

    public List<ToolCallback> select(List<ToolCallback> tools, String query) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }
        if (tools.size() <= maxCount || !StringUtils.hasText(query)) {
            return tools;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<Scored> scored = new ArrayList<>(tools.size());
        for (ToolCallback tool : tools) {
            scored.add(new Scored(tool, score(tool, normalized)));
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        List<ToolCallback> selected = new ArrayList<>(maxCount);
        for (int i = 0; i < scored.size() && i < maxCount; i++) {
            selected.add(scored.get(i).tool());
        }
        return selected;
    }

    /**
     * 粗排打分：命中的字符数。工具名比描述权重高——名字里带「天气」的比描述里提一句天气的更相关。
     */
    static int score(ToolCallback tool, String normalizedQuery) {
        String name = tool.getToolDefinition() == null ? "" : safe(tool.getToolDefinition().name());
        String description = tool.getToolDefinition() == null ? "" : safe(tool.getToolDefinition().description());
        int score = 0;
        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerDesc = description.toLowerCase(Locale.ROOT);
        for (int i = 0; i < normalizedQuery.length(); i++) {
            char c = normalizedQuery.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (lowerName.indexOf(c) >= 0) {
                score += 3;
            } else if (lowerDesc.indexOf(c) >= 0) {
                score += 1;
            }
        }
        return score;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Scored(ToolCallback tool, int score) {
    }
}