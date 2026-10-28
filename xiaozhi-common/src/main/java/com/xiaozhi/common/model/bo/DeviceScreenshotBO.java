package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备截图：调用设备截图工具后回传的图片地址与结果。
 */
@Data
public class DeviceScreenshotBO {

    public static final String STATE_PENDING = "0";
    public static final String STATE_SUCCESS = "1";
    public static final String STATE_FAILED = "2";

    private Long shotId;
    private String deviceId;
    private Long commandId;
    private String imageUrl;
    private String state;
    private String message;
    private Integer createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
