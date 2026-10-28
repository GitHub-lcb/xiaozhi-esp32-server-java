package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.util.List;

/**
 * 文档分片：检索的最小单位，embedding 为空表示还没向量化。
 */
@Data
public class KnowledgeChunkBO {

    private Long chunkId;
    private Long docId;
    private Integer knowledgeId;
    private Integer chunkIndex;
    private String content;
    private List<Double> embedding;
}
