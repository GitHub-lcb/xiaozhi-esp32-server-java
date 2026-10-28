package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备远程控制指令：把管理端的动作翻译成设备 MCP 工具调用，并记录执行结果。
 */
@Data
public class DeviceCommandBO {

    public static final String ACTION_REBOOT = "reboot";
    public static final String ACTION_VOLUME = "volume";
    public static final String ACTION_CUSTOM = "custom";

    public static final String STATE_SENT = "0";
    public static final String STATE_SUCCESS = "1";
    public static final String STATE_FAILED = "2";

    private Long commandId;
    private String deviceId;
    private String action;
    private String toolName;
    private String arguments;
    private String state;
    private String result;
    private Integer createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
