package com.xiaozhi.firmware.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 固件升级任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_firmware_release")
public class FirmwareReleaseDO extends BaseDO {

    @TableId(value = "releaseId", type = IdType.AUTO)
    private Integer releaseId;

    private Integer firmwareId;
    private String releaseName;
    private String strategy;
    private Integer percent;
    private String whitelist;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String state;
}
