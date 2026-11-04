package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StackChan 社区帖子响应，字段与 Go 版后端 GET /post/get 的 {@code model.Post} 对齐。
 * <p>
 * Java 字段名按本仓规范用 {@code createdTime}（时间字段不得以 At 结尾），
 * JSON 键仍是 Go 版的 {@code createdAt}，靠 {@link JsonProperty} 对齐，App/固件解析逻辑不用改。
 */
@Data
@Schema(description = "StackChan 社区帖子")
public class StackChanPostResp {

    @Schema(description = "帖子 ID")
    private Long id;

    @Schema(description = "发帖设备 MAC")
    private String mac;

    @Schema(description = "发帖设备名")
    private String name;

    @Schema(description = "文本内容")
    private String contentText;

    @Schema(description = "图片地址")
    private String contentImage;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发帖时间")
    private LocalDateTime createdTime;

    @Schema(description = "评论列表")
    private List<StackChanPostCommentResp> postCommentList;
}
