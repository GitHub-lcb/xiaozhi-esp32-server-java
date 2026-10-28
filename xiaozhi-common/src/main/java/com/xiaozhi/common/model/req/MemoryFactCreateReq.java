package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "长期记忆创建请求")
public class MemoryFactCreateReq {

    @Schema(description = "事实内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "记忆内容不能为空")
    @Size(max = 500, message = "记忆内容不能超过 500 字")
    private String content;

    @Schema(description = "说话人/成员，空表示全家")
    @Size(max = 50, message = "成员称呼过长")
    private String memberName;

    @Schema(description = "分类：偏好/日程/关系/其它")
    @Size(max = 50, message = "分类过长")
    private String category;
}
