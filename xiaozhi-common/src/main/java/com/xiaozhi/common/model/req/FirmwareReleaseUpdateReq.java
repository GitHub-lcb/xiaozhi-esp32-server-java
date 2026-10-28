package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "固件升级任务更新请求")
public class FirmwareReleaseUpdateReq {

    @Schema(description = "目标固件ID")
    private Integer firmwareId;

    @Schema(description = "任务名称")
    private String releaseName;

    @Schema(description = "投放策略：all percent whitelist")
    private String strategy;

    @Schema(description = "灰度百分比(1-100)")
    private Integer percent;

    @Schema(description = "白名单设备ID，逗号分隔")
    private String whitelist;

    @Schema(description = "时间窗开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "时间窗结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
