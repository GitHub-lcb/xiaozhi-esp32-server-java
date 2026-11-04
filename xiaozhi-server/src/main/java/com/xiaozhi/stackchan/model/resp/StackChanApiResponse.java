package com.xiaozhi.stackchan.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * StackChan 设备兼容接口响应体。
 * <p>
 * M5Stack 开源固件（StackChan Open-Source）解析的是 Go 版后端的
 * {@code {"code":0,"message":"","data":...}} 结构，成功码必须是 0，
 * 与 Java 管理端的 {@code ApiResponse}（成功码 200）刻意区分，避免改固件。
 */
@Getter
@Schema(description = "StackChan 设备兼容响应")
public class StackChanApiResponse<T> {

    @Schema(description = "0 成功，非 0 失败", example = "0")
    private final int code;

    @Schema(description = "提示信息")
    private final String message;

    @Schema(description = "业务数据")
    private final T data;

    private StackChanApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> StackChanApiResponse<T> success(T data) {
        return new StackChanApiResponse<>(0, "", data);
    }

    public static <T> StackChanApiResponse<T> error(int code, String message) {
        return new StackChanApiResponse<>(code, message, null);
    }
}
