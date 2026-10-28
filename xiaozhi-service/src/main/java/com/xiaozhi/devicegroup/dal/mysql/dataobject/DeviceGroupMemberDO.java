package com.xiaozhi.devicegroup.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_device_group_member")
public class DeviceGroupMemberDO extends BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private String deviceId;
}
