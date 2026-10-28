package com.xiaozhi.ai.knowledge;

import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.knowledge.service.KnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 知识库检索：把问题向量化后与库内分片算余弦相似度，取 topK。
 * <p>
 * 当前是进程内全量计算（配合 MySQL 存储），文档量到十万级再换向量库；
 * 先把链路跑通、把召回效果调好，比先上基础设施更重要。
 */
@Slf4j
@Component
public class KnowledgeRetriever {

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeIndexer knowledgeIndexer;

    /**
     * 在一批知识库里检索。
     */
    public List<KnowledgeChunkBO> search(List<KnowledgeBaseBO> bases, String query, int topK) {
        if (bases == null || bases.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }
        List<Integer> knowledgeIds = bases.stream().map(KnowledgeBaseBO::getKnowledgeId).toList();
        List<KnowledgeChunkBO> chunks = knowledgeService.listChunksByKnowledgeIds(knowledgeIds);
        if (chunks.isEmpty()) {
            return List.of();
        }
        List<Double> queryVector = knowledgeIndexer.embedQuery(bases.get(0), query);
        return chunks.stream()
            .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
            .map(chunk -> new Scored(chunk, EmbeddingMath.cosine(queryVector, chunk.getEmbedding())))
            .filter(scored -> scored.score > 0)
            .sorted(Comparator.comparingDouble((Scored scored) -> scored.score).reversed())
            .limit(Math.max(1, topK))
            .map(scored -> scored.chunk)
            .filter(Objects::nonNull)
            .toList();
    }

    private record Scored(KnowledgeChunkBO chunk, double score) {
    }
}
