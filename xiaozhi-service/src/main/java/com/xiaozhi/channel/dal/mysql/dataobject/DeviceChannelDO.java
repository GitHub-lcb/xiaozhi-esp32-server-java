package com.xiaozhi.channel.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设备接入通道。deviceId 即主键，一台设备只有一条记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_device_channel")
public class DeviceChannelDO extends BaseDO {

    @TableId(value = "deviceId", type = IdType.INPUT)
    private String deviceId;

    private String channel;
    private String clientId;
    private String onlineState;
    private LocalDateTime lastSeenTime;
}
