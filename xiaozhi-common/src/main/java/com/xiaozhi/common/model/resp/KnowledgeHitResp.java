package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识库检索命中")
public class KnowledgeHitResp {

    @Schema(description = "分片ID")
    private Long chunkId;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "知识库ID")
    private Integer knowledgeId;

    @Schema(description = "相似度 0-1")
    private Double score;

    @Schema(description = "分片内容")
    private String content;
}
