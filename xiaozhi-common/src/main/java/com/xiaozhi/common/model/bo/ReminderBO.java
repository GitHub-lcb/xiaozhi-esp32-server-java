package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音提醒/闹钟：到点让设备播报一句话，可一次性或按天/工作日重复。
 */
@Data
public class ReminderBO {

    public static final String REPEAT_ONCE = "once";
    public static final String REPEAT_DAILY = "daily";
    public static final String REPEAT_WEEKDAY = "weekday";

    public static final String STATE_PENDING = "0";
    public static final String STATE_FIRED = "1";
    public static final String STATE_CANCELED = "2";
    public static final String STATE_MISSED = "3";

    private Long reminderId;
    private String deviceId;
    private String title;
    private String content;
    private LocalDateTime remindTime;
    private String repeatType;
    private String state;
    private LocalDateTime lastFireTime;
    private Integer createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isRepeating() {
        return REPEAT_DAILY.equals(repeatType) || REPEAT_WEEKDAY.equals(repeatType);
    }
}
