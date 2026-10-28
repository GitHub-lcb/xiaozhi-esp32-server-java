package com.xiaozhi.ai.memory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 从一句事实里抽三元组（谁 关系 什么）。
 * <p>
 * 用的是关系词表 + 首词切分，不是 LLM：事实本身已经是短陈述句（"妈妈喜欢乌龙茶"），
 * 规则够用且零成本、可预测；句式复杂到规则切不动时返回空，不硬猜。
 */
@Component
public class MemoryGraphExtractor {

    /** 关系词按长度倒序匹配，避免"喜欢"被"喜"抢先命中 */
    private static final List<String> RELATIONS = List.of(
        "喜欢", "不喜欢", "讨厌", "爱喝", "爱吃", "爱", "住在", "住在", "工作", "上學", "上学",
        "的", "叫", "有", "是", "养了", "在"
    );

    public Optional<Triple> extract(String content) {
        if (!StringUtils.hasText(content)) {
            return Optional.empty();
        }
        String text = content.trim().replace("。", "").replace("，", "").replace(",", "");
        for (String relation : RELATIONS) {
            int index = text.indexOf(relation);
            if (index <= 0 || index + relation.length() >= text.length()) {
                continue;
            }
            String from = text.substring(0, index).trim();
            String to = text.substring(index + relation.length()).trim();
            if (!from.isEmpty() && !to.isEmpty() && from.length() <= 20 && to.length() <= 30) {
                return Optional.of(new Triple(from, relation, to));
            }
        }
        return Optional.empty();
    }

    public record Triple(String from, String relation, String to) {
    }
}