package com.xiaozhi.push.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_push_message")
public class PushMessageDO extends BaseDO {

    @TableId(value = "pushId", type = IdType.AUTO)
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
}
