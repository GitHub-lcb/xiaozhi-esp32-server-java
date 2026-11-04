package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * StackChan 帖子评论删除请求。
 */
@Data
@Schema(description = "StackChan 帖子评论删除请求")
public class StackChanPostCommentDeleteReq {

    @Schema(description = "帖子 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "postId cannot be empty")
    private Long postId;

    @Schema(description = "评论 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "commentId cannot be empty")
    private Long commentId;
}
