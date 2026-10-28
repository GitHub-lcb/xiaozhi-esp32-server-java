package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "固件升级记录")
public class FirmwareUpgradeLogResp {

    @Schema(description = "记录ID")
    private Long logId;

    @Schema(description = "升级任务ID")
    private Integer releaseId;

    @Schema(description = "升级任务名称")
    private String releaseName;

    @Schema(description = "固件ID")
    private Integer firmwareId;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "芯片型号")
    private String chipModel;

    @Schema(description = "升级前版本")
    private String fromVersion;

    @Schema(description = "升级目标版本")
    private String toVersion;

    @Schema(description = "设备IP")
    private String ip;

    @Schema(description = "状态(0已下发 1成功 2失败)")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
