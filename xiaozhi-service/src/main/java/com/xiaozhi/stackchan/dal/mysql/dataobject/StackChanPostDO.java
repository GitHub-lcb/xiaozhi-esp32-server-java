package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan 社区帖子：一台设备（MAC）发布的一条图文动态。
 * <p>
 * 对应 Go 版后端的 {@code device_post} 表；列名按 Java 侧约定改成驼峰，
 * 帖子只由设备自己删除，评论随帖子级联删除（Go 版是外键 ON DELETE CASCADE，这里由 Service 显式清理）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_post")
public class StackChanPostDO extends BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发帖设备 MAC，统一小写冒号格式 */
    private String mac;

    private String contentText;

    /** 图片地址 */
    private String contentImage;
}
