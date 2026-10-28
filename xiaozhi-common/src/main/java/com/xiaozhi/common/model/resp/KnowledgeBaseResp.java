package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库")
public class KnowledgeBaseResp {

    @Schema(description = "知识库ID")
    private Integer knowledgeId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "绑定角色ID")
    private Integer roleId;

    @Schema(description = "向量模型配置ID")
    private Integer embeddingConfigId;

    @Schema(description = "状态(1启用 0停用)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
