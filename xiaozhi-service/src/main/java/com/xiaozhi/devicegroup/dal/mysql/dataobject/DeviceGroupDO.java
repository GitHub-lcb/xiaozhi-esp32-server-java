package com.xiaozhi.devicegroup.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_device_group")
public class DeviceGroupDO extends BaseDO {

    @TableId(value = "groupId", type = IdType.AUTO)
    private Long groupId;

    private Integer userId;
    private String groupName;
}
