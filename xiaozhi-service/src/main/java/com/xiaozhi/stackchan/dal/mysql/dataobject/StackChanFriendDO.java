package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan 设备好友关系：一条有向记录（macA 加 macB 为好友）。
 * <p>
 * 对应 Go 版后端的 {@code device_friend} 表。Go 版主键是 (mac_a, mac_b)，
 * 这里用自增 id 做物理主键、唯一键仍落在 (macA, macB)，便于 MyBatis-Plus 按主键维护。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_friend")
public class StackChanFriendDO extends BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发起方设备 MAC，统一小写冒号格式 */
    private String macA;

    /** 好友设备 MAC，统一小写冒号格式 */
    private String macB;
}
