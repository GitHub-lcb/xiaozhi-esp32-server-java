package com.xiaozhi.stackchan.community;

import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * StackChan 社区接口（/post 系列、/pano、/friend）的异常收口。
 * <p>
 * 与 Go 版后端保持同一口径：业务错误仍是 HTTP 200 + {@code {"code":非0,"message":"..."}}，
 * 固件/App 只解析 body，回非 2xx 会被当成网络错误而看不到具体原因；
 * code 取 GoFrame {@code gcode} 的通用值（参数错误 51、服务器错误 50），客户端只判断 {@code code != 0}。
 * <p>
 * 只接管本包三个控制器，管理端接口的错误格式不受影响；显式声明最高优先级，
 * 确保不会落到全局 {@code GlobalExceptionHandler} 的 {@code ApiResponse}（成功码 200）结构上。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
    StackChanPostController.class,
    StackChanPanoController.class,
    StackChanFriendController.class
})
public class StackChanCommunityExceptionHandler {

    /** 与 GoFrame gcode.CodeInvalidParameter 数值一致 */
    private static final int CODE_INVALID_PARAMETER = 51;

    /** 与 GoFrame gcode.CodeInternalError 数值一致 */
    private static final int CODE_INTERNAL_ERROR = 50;

    @ExceptionHandler(IllegalArgumentException.class)
    public StackChanApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("StackChan 社区接口参数错误: {}", e.getMessage());
        return StackChanApiResponse.error(CODE_INVALID_PARAMETER,
            StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Invalid request parameter");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public StackChanApiResponse<Void> handleBindException(Exception e) {
        BindingResult bindingResult = e instanceof MethodArgumentNotValidException methodArgumentNotValidException
            ? methodArgumentNotValidException.getBindingResult()
            : ((BindException) e).getBindingResult();
        FieldError fieldError = bindingResult == null ? null : bindingResult.getFieldError();
        String message = fieldError != null && StringUtils.hasText(fieldError.getDefaultMessage())
            ? fieldError.getDefaultMessage()
            : "Invalid request parameter";
        log.warn("StackChan 社区接口参数校验失败: {}", message);
        return StackChanApiResponse.error(CODE_INVALID_PARAMETER, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public StackChanApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("StackChan 社区接口参数类型不匹配: name={}", e.getName());
        return StackChanApiResponse.error(CODE_INVALID_PARAMETER,
            "Invalid value for parameter: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public StackChanApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("StackChan 社区接口请求体解析失败: {}", e.getMessage());
        return StackChanApiResponse.error(CODE_INVALID_PARAMETER, "Invalid request body");
    }

    @ExceptionHandler(Exception.class)
    public StackChanApiResponse<Void> handleException(Exception e) {
        log.error("StackChan 社区接口处理异常", e);
        return StackChanApiResponse.error(CODE_INTERNAL_ERROR, "Server error, please try again later");
    }
}
