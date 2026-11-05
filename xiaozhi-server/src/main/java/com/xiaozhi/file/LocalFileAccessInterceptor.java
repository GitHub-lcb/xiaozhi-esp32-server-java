package com.xiaozhi.file;

import com.xiaozhi.common.web.LocalFileUrlPolicy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
/**
 * 本地存储静态目录的访问校验：受保护目录下只有带未过期签名的请求才放行，其余路径直接通过
 */
@Slf4j
@Component
public class LocalFileAccessInterceptor implements HandlerInterceptor {

    @Resource
    private LocalFileUrlPolicy localFileUrlPolicy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        boolean allowed;
        try {
            allowed = localFileUrlPolicy.verify(path, request.getParameter("exp"), request.getParameter("sig"));
        } catch (RuntimeException e) {
            log.error("文件访问签名校验异常: {}", path, e);
            allowed = false;
        }
        if (allowed) {
            return true;
        }
        log.warn("文件访问签名无效，已拒绝: {}", path);
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"访问地址无效或已过期\"}");
        return false;
    }

    /**
     * 取出用于验签的相对路径。
     * <p>
     * 必须把 URI 里的百分号编码还原成真实文件名再验签：下发签名时用的是磁盘上的原始文件名
     * （如 {@code Kevin MacLeod - Carefree.mp3}、{@code 测试.mp3}），而浏览器请求时会把空格编码成
     * {@code %20}、中文编码成 {@code %E6%B5%8B}，{@link HttpServletRequest#getRequestURI()} 返回的是
     * 编码后的形态。若不还原就会拿编码后的路径重算签名，必然对不上，试听/下载一律 403。
     * <p>
     * 只解码百分号 {@code %}，不把 {@code +} 当空格（与 {@link com.xiaozhi.stackchan.file.StackChanFileController}
     * 的路径还原保持一致）。非法编码时回退原文，交给验签流程拒绝。
     */
    static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        String rawPath = uri.startsWith("/") ? uri.substring(1) : uri;
        try {
            return UriUtils.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return rawPath;
        }
    }
}
