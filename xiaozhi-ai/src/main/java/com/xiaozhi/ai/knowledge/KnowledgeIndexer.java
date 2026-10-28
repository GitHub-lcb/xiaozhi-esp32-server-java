package com.xiaozhi.ai.knowledge;

import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.common.model.bo.KnowledgeDocBO;
import com.xiaozhi.knowledge.service.KnowledgeService;
import com.xiaozhi.storage.service.StorageServiceFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档入库：抽取正文 → 切片 → 向量化 → 落库。
 * <p>
 * 走同步执行（上传接口里直接做完），失败时把错误写回文档状态，
 * 管理端能直接看到「哪个文件为什么没进库」，不用翻日志。
 */
@Slf4j
@Component
public class KnowledgeIndexer {

    /** 全局默认向量模型配置ID；知识库自己配了就用知识库的 */
    @Value("${xiaozhi.knowledge.embedding-config-id:0}")
    private int defaultEmbeddingConfigId;

    /** 单次向量化的分片数，控制单次请求体量 */
    @Value("${xiaozhi.knowledge.embed-batch-size:16}")
    private int embedBatchSize = 16;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private DocumentTextExtractor documentTextExtractor;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @Resource
    private ChatModelFactory chatModelFactory;

    /**
     * @param docId 已落库的文档ID
     * @param base  文档所属知识库（用来取向量模型配置）
     */
    public void index(Long docId, KnowledgeBaseBO base) {
        KnowledgeDocBO doc = knowledgeService.listDocs(base.getKnowledgeId()).stream()
            .filter(item -> docId.equals(item.getDocId()))
            .findFirst()
            .orElse(null);
        if (doc == null) {
            log.warn("文档不存在，跳过索引 docId={}", docId);
            return;
        }
        try {
            String text = extractText(doc);
            List<String> pieces = TextChunker.chunk(text);
            if (pieces.isEmpty()) {
                knowledgeService.markDocState(docId, KnowledgeDocBO.STATE_FAILED, 0, "文档没有可索引的文本");
                return;
            }
            Integer configId = resolveConfigId(base);
            if (configId == null) {
                knowledgeService.markDocState(docId, KnowledgeDocBO.STATE_FAILED, 0,
                    "没有可用的向量模型配置，请在知识库或 xiaozhi.knowledge.embedding-config-id 中指定");
                return;
            }
            List<KnowledgeChunkBO> chunks = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i++) {
                KnowledgeChunkBO chunk = new KnowledgeChunkBO();
                chunk.setDocId(docId);
                chunk.setKnowledgeId(base.getKnowledgeId());
                chunk.setChunkIndex(i);
                chunk.setContent(pieces.get(i));
                chunk.setEmbedding(embed(configId, pieces.get(i)));
                chunks.add(chunk);
            }
            knowledgeService.saveChunks(chunks);
            knowledgeService.markDocState(docId, KnowledgeDocBO.STATE_READY, chunks.size(), null);
            log.info("文档索引完成 docId={} chunks={}", docId, chunks.size());
        } catch (Exception e) {
            log.error("文档索引失败 docId={}", docId, e);
            knowledgeService.markDocState(docId, KnowledgeDocBO.STATE_FAILED, 0, e.getMessage());
        }
    }

    /** 查询文本向量，供检索复用 */
    public List<Double> embedQuery(KnowledgeBaseBO base, String query) {
        Integer configId = resolveConfigId(base);
        if (configId == null) {
            throw new IllegalStateException("没有可用的向量模型配置");
        }
        return embed(configId, query);
    }

    private String extractText(KnowledgeDocBO doc) throws Exception {
        String storedPath = doc.getStoredPath();
        if (!StringUtils.hasText(storedPath)) {
            throw new IllegalStateException("文档没有存储路径");
        }
        byte[] bytes = storageServiceFactory.downloadFrom(storedPath);
        if (bytes == null) {
            throw new IllegalStateException("读取文档内容失败：" + storedPath);
        }
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return documentTextExtractor.extract(inputStream);
        }
    }

    private List<Double> embed(Integer configId, String text) {
        float[] vector = chatModelFactory.getEmbeddingModel(configId).embed(text);
        List<Double> values = new ArrayList<>(vector.length);
        for (float v : vector) {
            values.add((double) v);
        }
        return values;
    }

    private Integer resolveConfigId(KnowledgeBaseBO base) {
        if (base.getEmbeddingConfigId() != null && base.getEmbeddingConfigId() > 0) {
            return base.getEmbeddingConfigId();
        }
        return defaultEmbeddingConfigId > 0 ? defaultEmbeddingConfigId : null;
    }
}
