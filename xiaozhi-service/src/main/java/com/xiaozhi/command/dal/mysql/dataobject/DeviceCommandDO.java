package com.xiaozhi.command.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_device_command")
public class DeviceCommandDO extends BaseDO {

    @TableId(value = "commandId", type = IdType.AUTO)
    private Long commandId;

    private String deviceId;
    private String action;
    private String toolName;
    private String arguments;
    private String state;
    private String result;
    private Integer createUserId;
}
