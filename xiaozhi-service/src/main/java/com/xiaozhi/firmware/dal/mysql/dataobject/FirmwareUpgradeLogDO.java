package com.xiaozhi.firmware.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 固件升级记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_firmware_upgrade_log")
public class FirmwareUpgradeLogDO extends BaseDO {

    @TableId(value = "logId", type = IdType.AUTO)
    private Long logId;

    private Integer releaseId;
    private Integer firmwareId;
    private String deviceId;
    private String chipModel;
    private String fromVersion;
    private String toVersion;
    private String ip;
    private String status;
}
