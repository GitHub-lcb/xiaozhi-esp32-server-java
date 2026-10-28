package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "主动推送创建请求")
public class PushMessageCreateReq {

    @Schema(description = "推送标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @Schema(description = "播报内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "播报内容不能为空")
    @Size(max = 1000, message = "播报内容不能超过 1000 字")
    private String content;

    @Schema(description = "目标类型：device-按设备 user-按用户", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标类型不能为空")
    @Pattern(regexp = "^(device|user)$", message = "目标类型只能是 device 或 user")
    private String targetType;

    @Schema(description = "目标值：逗号分隔的设备ID或用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "推送目标不能为空")
    @Size(max = 2000, message = "目标列表过长")
    private String targetValue;
}
