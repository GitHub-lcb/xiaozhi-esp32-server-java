package com.xiaozhi.stackchan.store;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * StackChan App 侧登录态校验。
 * <p>
 * App 的网络层把 Sa-Token 放在 {@code token} 头里（值可能带 {@code Bearer } 前缀），
 * 与管理端用 {@code Authorization} 头的习惯不同；两种都认，避免 App 需要为跳舞等接口单独改请求头。
 * 设备请求不走这里，仍由 {@link StackChanDeviceAuth} 按 Device-Id + HMAC token 校验。
 */
@Component
public class StackChanAppAuth {

    /** App 网络层使用的 token 头名 */
    private static final String APP_TOKEN_HEADER = "token";

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 校验 App 登录态并返回登录用户 ID。
     * <p>
     * 未登录时走 {@link StpUtil#checkLogin()} 抛 NotLoginException，由全局异常处理器统一转成 HTTP 401。
     */
    public Object requireLogin(HttpServletRequest request) {
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginId();
        }
        String token = stripBearer(request.getHeader(APP_TOKEN_HEADER));
        if (StringUtils.hasText(token)) {
            Object loginId;
            try {
                // Sa-Token 在 token 非法、JWT 解析失败等场景会抛运行时异常，统一按未登录处理
                loginId = StpUtil.getLoginIdByToken(token);
            } catch (RuntimeException e) {
                loginId = null;
            }
            if (loginId != null) {
                return loginId;
            }
        }
        StpUtil.checkLogin();
        return StpUtil.getLoginId();
    }

    private String stripBearer(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        String token = rawToken.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token;
    }
}
