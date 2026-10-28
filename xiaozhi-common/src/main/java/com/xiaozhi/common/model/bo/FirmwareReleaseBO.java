package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件升级任务：把某个固件版本按策略、按时间窗投放给设备。
 */
@Data
public class FirmwareReleaseBO {

    public static final String STRATEGY_ALL = "all";
    public static final String STRATEGY_PERCENT = "percent";
    public static final String STRATEGY_WHITELIST = "whitelist";

    public static final String STATE_PENDING = "0";
    public static final String STATE_ACTIVE = "1";
    public static final String STATE_PAUSED = "2";
    public static final String STATE_FINISHED = "3";

    private Integer releaseId;
    private Integer firmwareId;
    private String releaseName;
    private String strategy;
    private Integer percent;
    /** 白名单设备ID，逗号分隔 */
    private String whitelist;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
