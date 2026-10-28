package com.xiaozhi.ai.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 相似度计算是召回排序的唯一依据，维度不一致或零向量必须返回「不相关」而不是抛异常。
 */
class EmbeddingMathTest {

    @Test
    void identicalVectorsScoreOne() {
        List<Double> a = List.of(1d, 2d, 3d);

        assertThat(EmbeddingMath.cosine(a, a)).isEqualTo(1d);
    }

    @Test
    void orthogonalVectorsScoreZero() {
        assertThat(EmbeddingMath.cosine(List.of(1d, 0d), List.of(0d, 1d))).isEqualTo(0d);
    }

    @Test
    void oppositeVectorsScoreMinusOne() {
        assertThat(EmbeddingMath.cosine(List.of(1d, 0d), List.of(-1d, 0d))).isEqualTo(-1d);
    }

    @Test
    void mismatchedDimensionsAreNotComparable() {
        assertThat(EmbeddingMath.cosine(List.of(1d, 2d), List.of(1d))).isEqualTo(-1d);
        assertThat(EmbeddingMath.cosine(null, List.of(1d))).isEqualTo(-1d);
        assertThat(EmbeddingMath.cosine(List.of(), List.of())).isEqualTo(-1d);
    }

    @Test
    void zeroVectorIsNotComparable() {
        assertThat(EmbeddingMath.cosine(List.of(0d, 0d), List.of(1d, 1d))).isEqualTo(-1d);
    }
}
