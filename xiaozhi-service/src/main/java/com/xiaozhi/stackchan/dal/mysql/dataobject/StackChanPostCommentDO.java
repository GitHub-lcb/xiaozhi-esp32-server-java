package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan 社区帖子评论：一条帖子下某个设备（MAC）的回复。
 * <p>
 * 对应 Go 版后端的 {@code device_post_comment} 表，列名按 Java 侧约定改成驼峰。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_post_comment")
public class StackChanPostCommentDO extends BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属帖子 ID */
    private Long postId;

    /** 评论设备 MAC，统一小写冒号格式 */
    private String mac;

    private String content;
}
