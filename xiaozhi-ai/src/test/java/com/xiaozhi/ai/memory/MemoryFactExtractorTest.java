package com.xiaozhi.ai.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 抽取结果的解析与去重：模型偶尔会带说明文字或代码围栏，解析必须容错；
 * 去重不能只看完全相等，否则"妈妈喜欢乌龙茶"和"妈妈喜欢喝乌龙茶"会记两条。
 */
class MemoryFactExtractorTest {

    @Test
    void parsesPlainArray() {
        var drafts = MemoryFactExtractor.parse("[{\"content\":\"妈妈喜欢乌龙茶\",\"memberName\":\"妈妈\",\"category\":\"偏好\"}]");

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).content()).isEqualTo("妈妈喜欢乌龙茶");
    }

    @Test
    void toleratesFencesAndChatter() {
        String raw = "好的，结果如下：\n\u0060\u0060\u0060json\n[{\"content\":\"家里有只猫\"}]\n\u0060\u0060\u0060";

        assertThat(MemoryFactExtractor.parse(raw)).hasSize(1);
    }

    @Test
    void returnsEmptyForGarbage() {
        assertThat(MemoryFactExtractor.parse(null)).isEmpty();
        assertThat(MemoryFactExtractor.parse("没有值得记的")).isEmpty();
        assertThat(MemoryFactExtractor.parse("[not json]")).isEmpty();
    }

    @Test
    void detectsNearDuplicates() {
        List<String> existing = List.of("妈妈喜欢乌龙茶");

        assertThat(MemoryFactExtractor.isDuplicate("妈妈喜欢喝乌龙茶", existing)).isTrue();
        assertThat(MemoryFactExtractor.isDuplicate("妈妈喜欢乌龙茶。", existing)).isTrue();
        assertThat(MemoryFactExtractor.isDuplicate("爸爸喜欢咖啡", existing)).isFalse();
    }

    @Test
    void blankCandidateIsTreatedAsDuplicate() {
        assertThat(MemoryFactExtractor.isDuplicate("   ", List.of("任意"))).isTrue();
    }
}
