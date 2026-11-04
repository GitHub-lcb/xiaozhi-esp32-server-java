package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * StackChan 帖子评论分页响应：Go 版后端 GET /post/comment/get 的 data 是 {@code {list, total}}。
 */
@Data
@Schema(description = "StackChan 帖子评论分页")
public class StackChanPostCommentPageResp {

    @Schema(description = "当前页评论列表")
    private List<StackChanPostCommentResp> list;

    @Schema(description = "评论总数")
    private Long total;
}
