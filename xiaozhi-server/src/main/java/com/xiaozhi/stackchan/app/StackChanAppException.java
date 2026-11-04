package com.xiaozhi.stackchan.app;

import org.springframework.http.HttpStatus;

/**
 * StackChan App 兼容接口的业务异常。
 * <p>
 * 错误码刻意对齐 Go 版后端（GoFrame {@code gcode}），App 端只认这几类：
 * <ul>
 *   <li>{@link #CODE_BUSINESS_VALIDATION_FAILED}：业务校验失败，App 读取 body 里的 message 展示；</li>
 *   <li>{@link #CODE_INTERNAL_ERROR}：服务端内部错误，HTTP 仍是 200，避免 Dio 抛异常后 App 无 UI 兜底；</li>
 *   <li>{@link #CODE_UNAUTHORIZED}：登录态失效，HTTP 401，App 拦截到 401 会执行本地登出。</li>
 * </ul>
 */
public class StackChanAppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Go 版 {@code gcode.CodeBusinessValidationFailed} = 300，App 登录/注册页按它做特判 */
    public static final int CODE_BUSINESS_VALIDATION_FAILED = 300;

    /** Go 版 {@code gcode.CodeInternalError} = 50 */
    public static final int CODE_INTERNAL_ERROR = 50;

    /** Go 版 V2TokenAuthMiddleware 的 401 响应体 code */
    public static final int CODE_UNAUTHORIZED = 401;

    private final int code;

    private final HttpStatus httpStatus;

    private StackChanAppException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static StackChanAppException business(String message) {
        return new StackChanAppException(CODE_BUSINESS_VALIDATION_FAILED, message, HttpStatus.OK);
    }

    public static StackChanAppException internal(String message) {
        return new StackChanAppException(CODE_INTERNAL_ERROR, message, HttpStatus.OK);
    }

    public static StackChanAppException unauthorized(String message) {
        return new StackChanAppException(CODE_UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED);
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
