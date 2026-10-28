package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "知识库创建/更新请求")
public class KnowledgeBaseSaveReq {

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "名称不能超过 100 字")
    private String name;

    @Schema(description = "说明")
    @Size(max = 500, message = "说明不能超过 500 字")
    private String description;

    @Schema(description = "绑定角色ID")
    private Integer roleId;

    @Schema(description = "向量模型配置ID")
    private Integer embeddingConfigId;

    @Schema(description = "状态(1启用 0停用)")
    private String state;
}
