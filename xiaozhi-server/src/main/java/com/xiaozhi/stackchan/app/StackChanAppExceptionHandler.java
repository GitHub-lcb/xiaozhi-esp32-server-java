package com.xiaozhi.stackchan.app;

import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * StackChan App 兼容接口的异常收口。
 * <p>
 * 作用于 {@link StackChanAppController} 与 {@link StackChanAiSettingsController}：把异常翻译成
 * Go 版 App 的 {@code {"code":...}} 响应。
 * 业务错误一律 HTTP 200（App 只解析 body，收到非 2xx 会抛 DioException 且页面无兜底）；
 * 只有登录态失效回 HTTP 401，App 拦截到 401 会清本地登录态。
 * <p>
 * 与全局 {@code GlobalExceptionHandler} 的关系：本类显式声明最高优先级，
 * 且通过 assignableTypes 限定只接管这两个控制器，管理端其它接口的错误格式不受影响。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {StackChanAppController.class, StackChanAiSettingsController.class})
public class StackChanAppExceptionHandler {

    @ExceptionHandler(StackChanAppException.class)
    public ResponseEntity<StackChanApiResponse<Void>> handleStackChanAppException(StackChanAppException e) {
        return ResponseEntity.status(e.getHttpStatus())
            .body(StackChanApiResponse.<Void>error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StackChanApiResponse<Void>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e) {
        log.warn("StackChan App 请求体解析失败: {}", e.getMessage());
        return ResponseEntity.ok(StackChanApiResponse.<Void>error(
            StackChanAppException.CODE_BUSINESS_VALIDATION_FAILED, "Invalid request body."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StackChanApiResponse<Void>> handleException(Exception e) {
        log.error("StackChan App 接口处理异常", e);
        return ResponseEntity.ok(StackChanApiResponse.<Void>error(
            StackChanAppException.CODE_INTERNAL_ERROR, "Server error, please try again later."));
    }
}
