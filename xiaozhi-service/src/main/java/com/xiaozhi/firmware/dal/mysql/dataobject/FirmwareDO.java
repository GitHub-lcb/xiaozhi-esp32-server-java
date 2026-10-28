package com.xiaozhi.firmware.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备固件版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_firmware")
public class FirmwareDO extends BaseDO {

    @TableId(value = "firmwareId", type = IdType.AUTO)
    private Integer firmwareId;

    private String version;
    private String chipModel;
    private String fileUrl;
    private Long fileSize;
    private String md5;
    private String releaseNotes;
    private String state;
}
