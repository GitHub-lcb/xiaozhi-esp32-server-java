package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "语音提醒创建请求")
public class ReminderCreateReq {

    @Schema(description = "目标设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标设备不能为空")
    private String deviceId;

    @Schema(description = "提醒标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @Schema(description = "播报内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "播报内容不能为空")
    @Size(max = 500, message = "播报内容不能超过 500 字")
    private String content;

    @Schema(description = "首次播报时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "播报时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindTime;

    @Schema(description = "重复方式：once/daily/weekday，默认 once")
    @Pattern(regexp = "^(once|daily|weekday)$", message = "重复方式只能是 once / daily / weekday")
    private String repeatType;
}
