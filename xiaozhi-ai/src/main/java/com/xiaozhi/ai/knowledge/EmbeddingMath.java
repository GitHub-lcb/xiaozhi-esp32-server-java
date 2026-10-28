package com.xiaozhi.ai.knowledge;

import java.util.List;

/**
 * 向量相似度：MySQL 里没有向量索引，检索走全量内存计算，所以这里的实现要能处理空向量与维度不一致。
 */
public final class EmbeddingMath {

    private EmbeddingMath() {
    }

    /**
     * 余弦相似度，范围 [-1, 1]；任一向量为空或维度不一致返回 -1（视为不相关）。
     */
    public static double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || a.size() != b.size()) {
            return -1d;
        }
        double dot = 0d;
        double normA = 0d;
        double normB = 0d;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i) == null ? 0d : a.get(i);
            double y = b.get(i) == null ? 0d : b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0d || normB == 0d) {
            return -1d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
