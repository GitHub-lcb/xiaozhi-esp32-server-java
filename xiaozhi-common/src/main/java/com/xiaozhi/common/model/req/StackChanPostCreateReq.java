package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * StackChan 社区帖子创建请求。
 * <p>
 * JSON 键沿用 Go 版后端 / App 现有的 snake_case（{@code content_text}/{@code content_image}），
 * Java 字段名按本仓规范保持驼峰，靠 {@link JsonProperty} 对齐。
 */
@Data
@Schema(description = "StackChan 帖子创建请求")
public class StackChanPostCreateReq {

    @JsonProperty("content_text")
    @JsonAlias("contentText")
    @Schema(description = "文本内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "content_text cannot be empty")
    private String contentText;

    @JsonProperty("content_image")
    @JsonAlias("contentImage")
    @Schema(description = "图片地址")
    private String contentImage;
}
