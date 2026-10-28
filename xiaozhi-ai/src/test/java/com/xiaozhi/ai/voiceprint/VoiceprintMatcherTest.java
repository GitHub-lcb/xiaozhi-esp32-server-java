package com.xiaozhi.ai.voiceprint;

import com.xiaozhi.common.model.bo.VoiceprintBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 声纹认人：宁可认不出，也不要认错人。阈值以下必须返回未命中，且永远取最像的那一个。
 */
class VoiceprintMatcherTest {

    @Test
    void emptyCandidatesNeverMatch() {
        assertThat(VoiceprintMatcher.best(List.of(1d, 0d), List.of(), 0.6d)).isEmpty();
        assertThat(VoiceprintMatcher.best(null, List.of(voiceprint("爸爸", List.of(1d, 0d))), 0.6d)).isEmpty();
    }

    @Test
    void picksTheMostSimilarCandidate() {
        VoiceprintBO dad = voiceprint("爸爸", List.of(1d, 0d));
        VoiceprintBO mom = voiceprint("妈妈", List.of(0.7d, 0.7d));

        var match = VoiceprintMatcher.best(List.of(1d, 0.05d), List.of(mom, dad), 0.6d);

        assertThat(match).isPresent();
        assertThat(match.get().voiceprint().getMemberName()).isEqualTo("爸爸");
        assertThat(match.get().matched()).isTrue();
        assertThat(match.get().score()).isGreaterThan(0.9d);
    }

    @Test
    void belowThresholdIsNotMatchedEvenIfItIsTheClosest() {
        VoiceprintBO dad = voiceprint("爸爸", List.of(1d, 0d));

        var match = VoiceprintMatcher.best(List.of(1d, 1d), List.of(dad), 0.9d);

        assertThat(match).isPresent();
        assertThat(match.get().matched()).isFalse();
        assertThat(match.get().score()).isLessThan(0.9d);
    }

    @Test
    void candidatesWithoutEmbeddingAreSkipped() {
        VoiceprintBO broken = voiceprint("坏数据", null);
        VoiceprintBO dad = voiceprint("爸爸", List.of(1d, 0d));

        var match = VoiceprintMatcher.best(List.of(1d, 0d), List.of(broken, dad), 0.6d);

        assertThat(match).isPresent();
        assertThat(match.get().voiceprint().getMemberName()).isEqualTo("爸爸");
    }

    private VoiceprintBO voiceprint(String name, List<Double> embedding) {
        VoiceprintBO bo = new VoiceprintBO();
        bo.setVoiceprintId(1L);
        bo.setMemberName(name);
        bo.setEmbedding(embedding);
        return bo;
    }
}
