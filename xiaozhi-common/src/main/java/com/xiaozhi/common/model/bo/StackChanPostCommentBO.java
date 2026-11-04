package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 社区帖子评论。
 */
@Data
public class StackChanPostCommentBO {

    private Long id;

    private Long postId;

    /** 评论设备 MAC，统一小写冒号格式 */
    private String mac;

    private String content;

    private LocalDateTime createTime;
}
