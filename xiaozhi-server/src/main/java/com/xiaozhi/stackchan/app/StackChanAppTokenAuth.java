package com.xiaozhi.stackchan.app;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * StackChan App 兼容接口的登录态校验。
 * <p>
 * App 登录成功后会拿到管理端同一个 Sa-Token，之后每个请求放在 {@code token} 头里；
 * 兼容 Go 版的 {@code Authorization: Bearer <token>} 写法，两种位置、带不带 Bearer 前缀都接受。
 * <p>
 * 这些接口统一标注 {@link cn.dev33.satoken.annotation.SaIgnore}，绕过管理端拦截器，登录态只在这里判定。
 */
@Slf4j
@Component
public class StackChanAppTokenAuth {

    private static final String TOKEN_HEADER = "token";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 取当前登录用户 ID。
     *
     * @throws StackChanAppException 未带 token、token 无效或已过期，HTTP 401
     */
    public Integer requireUserId(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw StackChanAppException.unauthorized("The token cannot be empty.");
        }

        Object loginId;
        try {
            loginId = StpUtil.getLoginIdByToken(token);
        } catch (RuntimeException e) {
            // Sa-Token 在 token 非法、JWT 解析失败等场景可能直接抛异常，统一按未登录处理
            log.debug("StackChan App token 校验异常", e);
            throw StackChanAppException.unauthorized("The token is invalid or has expired.");
        }
        if (loginId == null) {
            throw StackChanAppException.unauthorized("The token is invalid or has expired.");
        }

        try {
            return Integer.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            throw StackChanAppException.unauthorized("The user in the token is invalid.");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);
        if (!StringUtils.hasText(token)) {
            token = request.getHeader(AUTHORIZATION_HEADER);
        }
        return stripBearer(token);
    }

    private String stripBearer(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String token = value.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return StringUtils.hasText(token) ? token : null;
    }
}
