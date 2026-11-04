package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan 设备全景图：一台设备（MAC）上传的一张 360 全景图地址。
 * <p>
 * 对应 Go 版后端的 {@code device_pano} 表，列名按 Java 侧约定改成驼峰。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_pano")
public class StackChanPanoDO extends BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 设备 MAC，统一小写冒号格式 */
    private String mac;

    /** 全景图地址 */
    private String panoUrl;
}
