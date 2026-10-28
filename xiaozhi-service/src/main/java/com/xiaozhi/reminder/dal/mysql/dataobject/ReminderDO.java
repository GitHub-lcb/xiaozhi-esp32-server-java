package com.xiaozhi.reminder.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_reminder")
public class ReminderDO extends BaseDO {

    @TableId(value = "reminderId", type = IdType.AUTO)
    private Long reminderId;

    private String deviceId;
    private String title;
    private String content;
    private LocalDateTime remindTime;
    private String repeatType;
    private String state;
    private LocalDateTime lastFireTime;
    private Integer createUserId;
}
