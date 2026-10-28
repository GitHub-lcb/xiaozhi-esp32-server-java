package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件升级记录：每次给设备下发升级都留一条。
 */
@Data
public class FirmwareUpgradeLogBO {

    public static final String STATUS_DELIVERED = "0";
    public static final String STATUS_SUCCESS = "1";
    public static final String STATUS_FAILED = "2";

    private Long logId;
    private Integer releaseId;
    private Integer firmwareId;
    private String deviceId;
    private String chipModel;
    private String fromVersion;
    private String toVersion;
    private String ip;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
