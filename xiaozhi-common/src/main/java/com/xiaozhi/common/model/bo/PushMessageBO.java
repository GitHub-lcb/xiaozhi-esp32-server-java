package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主动推送消息：服务端主动让设备播报一段文本（提醒、通知）。
 */
@Data
public class PushMessageBO {

    public static final String TARGET_DEVICE = "device";
    public static final String TARGET_USER = "user";

    public static final String STATE_PENDING = "0";
    public static final String STATE_SENT = "1";
    public static final String STATE_CANCELED = "2";

    private Long pushId;
    private String title;
    private String content;
    private String targetType;
    private String targetValue;
    private String state;
    private Integer totalCount;
    private Integer sentCount;
    private Integer offlineCount;
    private LocalDateTime sentTime;
    private Integer createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
