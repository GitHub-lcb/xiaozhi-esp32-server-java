package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "设备远程控制请求")
public class DeviceCommandReq {

    @Schema(description = "动作：reboot-重启 volume-设置音量 custom-自定义工具", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "动作不能为空")
    @Pattern(regexp = "^(reboot|volume|custom)$", message = "动作只能是 reboot / volume / custom")
    private String action;

    @Schema(description = "音量(0-100)，action=volume 时必填")
    @Min(value = 0, message = "音量不能小于 0")
    @Max(value = 100, message = "音量不能大于 100")
    private Integer volume;

    @Schema(description = "自定义工具名，action=custom 时必填")
    @Size(max = 100, message = "工具名过长")
    private String toolName;

    @Schema(description = "自定义工具入参 JSON，action=custom 时使用")
    @Size(max = 1000, message = "入参过长")
    private String arguments;
}
