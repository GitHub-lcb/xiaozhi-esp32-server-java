package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 社区帖子评论响应，字段与 Go 版后端 {@code model.PostComment} 对齐。
 * <p>
 * Java 字段名用 {@code createdTime}，JSON 键是 Go 版的 {@code createdAt}。
 */
@Data
@Schema(description = "StackChan 社区帖子评论")
public class StackChanPostCommentResp {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "帖子 ID")
    private Long postId;

    @Schema(description = "评论设备 MAC")
    private String mac;

    @Schema(description = "评论设备名")
    private String name;

    @Schema(description = "评论内容")
    private String content;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "评论时间")
    private LocalDateTime createdTime;
}
