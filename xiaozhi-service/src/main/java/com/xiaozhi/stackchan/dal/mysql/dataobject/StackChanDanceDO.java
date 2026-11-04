package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan 设备舞蹈：一台设备（MAC）下的一组舞蹈动作。
 * <p>
 * danceData 存的是 App/固件约定的动作 JSON 数组原文，服务端不解构、不重排，
 * 保证「写进去什么、读出来什么」。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_dance")
public class StackChanDanceDO extends BaseDO {

    @TableId(value = "danceId", type = IdType.AUTO)
    private Long danceId;

    /** 设备 MAC，统一小写冒号格式 */
    private String mac;

    private String danceName;

    /** 动作数据 JSON 数组原文 */
    private String danceData;

    private String musicUrl;
}
