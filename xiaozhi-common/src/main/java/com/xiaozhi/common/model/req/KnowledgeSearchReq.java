package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "知识库检索测试请求")
public class KnowledgeSearchReq {

    @Schema(description = "知识库ID，空表示检索全部启用的知识库", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer knowledgeId;

    @Schema(description = "查询内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "查询内容不能为空")
    private String query;

    @Schema(description = "返回条数，默认 5")
    private Integer topK;
}
