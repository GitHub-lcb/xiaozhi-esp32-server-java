package com.xiaozhi.common.config;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * 运维端点（Actuator、接口文档）的访问控制：一律要求登录态。
 * <p>
 * 这些路径不在 {@code /api/**} 之下，业务鉴权拦截器覆盖不到，与设备/业务端口又是同一个端口对外。
 * 用过滤器而不是 MVC 拦截器：Actuator 的端点挂在自己的 HandlerMapping 上，
 * {@code addInterceptors} 注册的拦截器不会作用到它们。server 与 dialogue 都会装配本配置。
 * <p>
 * Prometheus 抓取不可能带登录态，所以允许用运维令牌（{@code XIAOZHI_OPS_TOKEN}）访问
 * {@code /actuator/health} 与 {@code /actuator/prometheus} 这两个只读端点；
 * 令牌为空时整条捷径关闭，退化成「必须登录」。
 */
@Configuration
public class OpsEndpointAccessConfig {

    /** Servlet 的 {@code /x/*} 同时匹配 /x 与 /x/... ，端点根路径不必再单列 */
    private static final String[] OPS_URL_PATTERNS = {
            "/actuator/*",
            "/v3/api-docs/*", "/v3/api-docs.yaml",
            "/swagger-ui/*", "/swagger-ui.html",
            "/swagger-resources/*",
            "/doc.html"
    };

    /** 必须排在 Spring 的 RequestContextFilter（-105）之后：Sa-Token 取登录态要用它绑定的当前请求 */
    private static final int FILTER_ORDER = -100;

    /** 允许用运维令牌访问的只读端点，其余 actuator 端点仍然只认登录态 */
    private static final Set<String> TOKEN_ALLOWED_PATHS = Set.of(
            "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness",
            "/actuator/prometheus");

    /** 运维令牌，留空表示不支持令牌访问 */
    @Value("${xiaozhi.ops.token:}")
    private String opsToken;

    @Bean
    public FilterRegistrationBean<OpsEndpointAccessFilter> opsEndpointAccessFilter() {
        FilterRegistrationBean<OpsEndpointAccessFilter> registration =
                new FilterRegistrationBean<>(new OpsEndpointAccessFilter(opsToken));
        registration.addUrlPatterns(OPS_URL_PATTERNS);
        registration.setOrder(FILTER_ORDER);
        return registration;
    }

    public static class OpsEndpointAccessFilter extends OncePerRequestFilter {

        private static final String OPS_TOKEN_HEADER = "X-Ops-Token";
        private static final String BEARER_PREFIX = "Bearer ";

        private final String opsToken;

        public OpsEndpointAccessFilter(String opsToken) {
            this.opsToken = opsToken;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isLoggedIn()) {
                filterChain.doFilter(request, response);
                return;
            }
            if (hasValidOpsToken(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问\"}");
        }

        /**
         * 令牌只对只读的指标与健康端点生效，且必须与配置完全一致；
         * 比较走 {@link MessageDigest#isEqual}，避免按前缀逐字节比较带来的时序差异。
         */
        private boolean hasValidOpsToken(HttpServletRequest request) {
            if (!StringUtils.hasText(opsToken)) {
                return false;
            }
            String path = request.getRequestURI();
            if (!TOKEN_ALLOWED_PATHS.contains(path)) {
                return false;
            }
            String provided = request.getHeader(OPS_TOKEN_HEADER);
            if (!StringUtils.hasText(provided)) {
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                    provided = authorization.substring(BEARER_PREFIX.length());
                }
            }
            if (!StringUtils.hasText(provided)) {
                return false;
            }
            return MessageDigest.isEqual(opsToken.getBytes(StandardCharsets.UTF_8),
                    provided.getBytes(StandardCharsets.UTF_8));
        }

        /** 未登录属正常情况，不能让取登录态的异常冒到调用方 */
        private boolean isLoggedIn() {
            try {
                return StpUtil.getLoginIdDefaultNull() != null;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
