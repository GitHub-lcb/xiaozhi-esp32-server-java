package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * StackChan 全景图创建请求：Go 版后端的 JSON 键是 {@code url}。
 */
@Data
@Schema(description = "StackChan 全景图创建请求")
public class StackChanPanoCreateReq {

    @Schema(description = "全景图地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "url cannot be empty")
    private String url;
}
