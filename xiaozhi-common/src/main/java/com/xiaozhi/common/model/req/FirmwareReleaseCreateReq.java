package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "固件升级任务创建请求")
public class FirmwareReleaseCreateReq {

    @Schema(description = "目标固件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标固件不能为空")
    private Integer firmwareId;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    private String releaseName;

    @Schema(description = "投放策略：all-全量 percent-按比例 whitelist-白名单，默认 all")
    private String strategy;

    @Schema(description = "灰度百分比(1-100)，strategy=percent 时必填")
    private Integer percent;

    @Schema(description = "白名单设备ID，逗号分隔，strategy=whitelist 时必填")
    private String whitelist;

    @Schema(description = "时间窗开始，空表示立即")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "时间窗结束，空表示不限")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
