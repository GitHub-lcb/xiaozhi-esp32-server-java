package com.xiaozhi.ai.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则抽取的边界：能切出"谁 关系 什么"就返回，切不出来宁可返回空也不乱猜。
 */
class MemoryGraphExtractorTest {

    private final MemoryGraphExtractor extractor = new MemoryGraphExtractor();

    @Test
    void extractsSubjectRelationObject() {
        var triple = extractor.extract("妈妈喜欢乌龙茶");

        assertThat(triple).isPresent();
        assertThat(triple.get().from()).isEqualTo("妈妈");
        assertThat(triple.get().relation()).isEqualTo("喜欢");
        assertThat(triple.get().to()).isEqualTo("乌龙茶");
    }

    @Test
    void stripsTrailingPunctuation() {
        var triple = extractor.extract("爸爸住在杭州。");

        assertThat(triple).isPresent();
        assertThat(triple.get().to()).isEqualTo("杭州");
    }

    @Test
    void returnsEmptyWhenNoRelationWord() {
        assertThat(extractor.extract("今天天气不错")).isEmpty();
        assertThat(extractor.extract("喜欢")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}