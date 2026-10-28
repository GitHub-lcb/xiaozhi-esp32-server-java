package com.xiaozhi.ai.voiceprint;

import com.xiaozhi.ai.knowledge.EmbeddingMath;
import com.xiaozhi.common.model.bo.VoiceprintBO;

import java.util.List;
import java.util.Optional;

/**
 * 声纹比对：在同一个用户的候选声纹里找最像的一个，超过阈值才算认出。
 * <p>
 * 用最近邻而不是聚类/多原型：家庭成员数量少，单条注册向量足够；
 * 阈值默认取 0.62（余弦），是同类模型上常见的保守值，宁可认不出也不要认错人。
 */
public final class VoiceprintMatcher {

    public static final double DEFAULT_THRESHOLD = 0.62d;

    private VoiceprintMatcher() {
    }

    public static Optional<Match> best(List<Double> probe, List<VoiceprintBO> candidates, double threshold) {
        if (probe == null || probe.isEmpty() || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        VoiceprintBO bestCandidate = null;
        double bestScore = -1d;
        for (VoiceprintBO candidate : candidates) {
            if (candidate == null || candidate.getEmbedding() == null) {
                continue;
            }
            double score = EmbeddingMath.cosine(probe, candidate.getEmbedding());
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }
        if (bestCandidate == null) {
            return Optional.empty();
        }
        return Optional.of(new Match(bestCandidate, bestScore, bestScore >= threshold));
    }

    public record Match(VoiceprintBO voiceprint, double score, boolean matched) {
    }
}
