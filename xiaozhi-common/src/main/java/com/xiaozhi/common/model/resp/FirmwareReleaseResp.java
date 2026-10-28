package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "固件升级任务")
public class FirmwareReleaseResp {

    @Schema(description = "任务ID")
    private Integer releaseId;

    @Schema(description = "目标固件ID")
    private Integer firmwareId;

    @Schema(description = "目标固件版本号")
    private String firmwareVersion;

    @Schema(description = "任务名称")
    private String releaseName;

    @Schema(description = "投放策略：all-全量 percent-按比例 whitelist-白名单")
    private String strategy;

    @Schema(description = "灰度百分比(1-100)")
    private Integer percent;

    @Schema(description = "白名单设备ID，逗号分隔")
    private String whitelist;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间窗开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间窗结束")
    private LocalDateTime endTime;

    @Schema(description = "状态(0待开始 1进行中 2已暂停 3已结束)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
