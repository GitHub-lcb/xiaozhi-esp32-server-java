package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * StackChan 帖子删除请求。
 */
@Data
@Schema(description = "StackChan 帖子删除请求")
public class StackChanPostDeleteReq {

    @Schema(description = "帖子 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "id cannot be empty")
    private Long id;
}
