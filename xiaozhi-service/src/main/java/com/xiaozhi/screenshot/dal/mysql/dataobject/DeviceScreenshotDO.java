package com.xiaozhi.screenshot.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_device_screenshot")
public class DeviceScreenshotDO extends BaseDO {

    @TableId(value = "shotId", type = IdType.AUTO)
    private Long shotId;

    private String deviceId;
    private Long commandId;
    private String imageUrl;
    private String state;
    private String message;
    private Integer createUserId;
}
