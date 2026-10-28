package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "记忆图谱关系创建请求")
public class MemoryRelationCreateReq {

    @Schema(description = "起点实体名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "起点实体不能为空")
    @Size(max = 100, message = "实体名过长")
    private String fromName;

    @Schema(description = "关系名，如 喜欢", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关系不能为空")
    @Size(max = 50, message = "关系名过长")
    private String relation;

    @Schema(description = "终点实体名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "终点实体不能为空")
    @Size(max = 100, message = "实体名过长")
    private String toName;
}
