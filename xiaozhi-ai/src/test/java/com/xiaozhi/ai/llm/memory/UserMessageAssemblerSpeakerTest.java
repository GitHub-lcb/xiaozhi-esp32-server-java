package com.xiaozhi.ai.llm.memory;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 前缀字段顺序是约定好的：时间 → 说话人 → 情绪。说话人缺失时不能留下空方括号，
 * 否则模型会看到一个没有主语的 "[]"，反而干扰理解。
 */
class UserMessageAssemblerSpeakerTest {

    @Test
    void speakerSitsBetweenTimestampAndEmotion() {
        String text = UserMessageAssembler.assemble("今天天气怎么样", Instant.parse("2026-10-28T02:00:00Z"), "妈妈", "happy");

        assertThat(text).contains("[说话人:妈妈]");
        assertThat(text.indexOf("[说话人:妈妈]")).isGreaterThan(text.indexOf("[2026-"));
        assertThat(text.indexOf("[happy]")).isGreaterThan(text.indexOf("[说话人:妈妈]"));
        assertThat(text).endsWith("今天天气怎么样");
    }

    @Test
    void missingSpeakerKeepsPrefixAsBefore() {
        String text = UserMessageAssembler.assemble("你好", null, null, "neutral");

        assertThat(text).isEqualTo("[neutral] 你好");
        assertThat(text).doesNotContain("说话人");
    }

    @Test
    void blankSpeakerIsSkipped() {
        String text = UserMessageAssembler.assemble("你好", null, "   ", null);

        assertThat(text).isEqualTo("你好");
    }
}
