package com.xiaozhi.ai.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工具调用安抚词：模型去调工具（查天气、开灯）时先垫一句话，避免设备静默几百毫秒到几秒。
 * <p>
 * 文案可配；留空即关闭。只在同一轮第一次调用工具时说一次，连续多次工具调用不重复念。
 */
@Component
public class ToolFillerProvider {

    /** 逗号分隔的文案列表，留空关闭 */
    @Value("${xiaozhi.dialogue.tool-fillers:稍等，我查一下}")
    private String configured;

    public boolean enabled() {
        return StringUtils.hasText(configured);
    }

    /** 随机取一句；关闭时返回 null */
    public String next() {
        if (!enabled()) {
            return null;
        }
        List<String> phrases = Arrays.stream(configured.split("[,，]"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
        if (phrases.isEmpty()) {
            return null;
        }
        return phrases.get(ThreadLocalRandom.current().nextInt(phrases.size()));
    }

    /** 测试用：直接指定文案 */
    void setConfigured(String configured) {
        this.configured = configured;
    }
}