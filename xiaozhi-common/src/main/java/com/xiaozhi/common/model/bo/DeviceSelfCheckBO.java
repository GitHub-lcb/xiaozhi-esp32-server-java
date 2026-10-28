package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备接入一键自检结果。
 */
@Data
public class DeviceSelfCheckBO {

    public static final String SUMMARY_PASS = "pass";
    public static final String SUMMARY_WARN = "warn";
    public static final String SUMMARY_FAIL = "fail";

    private String deviceId;
    private String deviceName;
    /** 总体结论：有 fail 即 fail，否则有 warn 即 warn，否则 pass */
    private String summary;
    private int passed;
    private int warned;
    private int failed;
    private LocalDateTime checkTime;
    private List<SelfCheckItemBO> items;
}
