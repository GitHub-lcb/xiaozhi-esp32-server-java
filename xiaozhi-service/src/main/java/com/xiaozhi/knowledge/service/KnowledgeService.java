package com.xiaozhi.knowledge.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.common.model.bo.KnowledgeDocBO;

import java.util.List;

/**
 * 知识库的存储层：库、文档、分片的读写。解析、向量化和检索在 xiaozhi-ai 里做。
 */
public interface KnowledgeService {

    PageResult<KnowledgeBaseBO> page(int pageNo, int pageSize, String name);

    KnowledgeBaseBO getBO(Integer knowledgeId);

    List<KnowledgeBaseBO> listEnabled();

    /** 绑定到某个角色的启用知识库 */
    List<KnowledgeBaseBO> listByRole(Integer roleId);

    KnowledgeBaseBO create(KnowledgeBaseBO bo);

    KnowledgeBaseBO update(Integer knowledgeId, KnowledgeBaseBO bo);

    void delete(Integer knowledgeId);

    KnowledgeDocBO createDoc(KnowledgeDocBO bo);

    List<KnowledgeDocBO> listDocs(Integer knowledgeId);

    void markDocState(Long docId, String state, int chunkCount, String errorMsg);

    void deleteDoc(Long docId);

    void saveChunks(List<KnowledgeChunkBO> chunks);

    List<KnowledgeChunkBO> listChunksByKnowledge(Integer knowledgeId);

    List<KnowledgeChunkBO> listChunksByKnowledgeIds(List<Integer> knowledgeIds);
}
