package com.xiaozhi.stackchan.ws;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.utils.CommonUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * StackChan 伴侣 WebSocket 握手鉴权。
 * <p>
 * 同一个 {@code /api/stackChan/ws} 端点按 {@code deviceType} 分流：
 * <ul>
 *   <li>{@code StackChan}：固件，用 {@link StackChanDeviceAuth} 校验 Device-Id + HMAC token
 *       （未配置 {@code xiaozhi.device-auth.secret} 时与 xiaozhi WS 一致地只要求 Device-Id）；</li>
 *   <li>{@code App}：改造后的 Flutter App，Device-Id 头是目标 StackChan 的 MAC，
 *       Authorization 里是管理端 Sa-Token（Bearer 前缀可选），校验通过后还要确认设备归属当前登录用户。</li>
 * </ul>
 * 鉴权结论只在这里判定一次，写进握手属性，连接建立之后一律从属性取，不再二次解析请求头。
 * 失败返回 401 并拒绝升级。
 */
@Slf4j
@Component
public class StackChanWsHandshakeInterceptor implements HandshakeInterceptor {

    /** 握手属性：本连接的端类型，值为 {@link #DEVICE_TYPE_STACK_CHAN} 或 {@link #DEVICE_TYPE_APP} */
    public static final String ATTR_DEVICE_TYPE = "deviceType";
    /** 握手属性：目标 StackChan 的小写冒号 MAC（设备端与 App 端都会写） */
    public static final String ATTR_DEVICE_ID = "deviceId";
    /** 握手属性：App 实例标识（App 查询参数 deviceId；当前 Flutter 版是 App 实例 UUID） */
    public static final String ATTR_APP_DEVICE_ID = "appDeviceId";
    /** 握手属性：App 登录用户 ID（Integer） */
    public static final String ATTR_USER_ID = "userId";

    public static final String DEVICE_TYPE_STACK_CHAN = "StackChan";
    public static final String DEVICE_TYPE_APP = "App";

    private static final String HEADER_DEVICE_ID = "Device-Id";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_TOKEN = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private DeviceService deviceService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            Map<String, String> query = parseQuery(request.getURI().getRawQuery());
            String deviceType = query.getOrDefault("deviceType", "").trim();
            if (!StringUtils.hasText(deviceType)) {
                return reject(response, "缺少 deviceType 参数", null);
            }
            if (DEVICE_TYPE_STACK_CHAN.equalsIgnoreCase(deviceType)) {
                return authenticateStackChan(request, response, attributes);
            }
            if (DEVICE_TYPE_APP.equalsIgnoreCase(deviceType)) {
                return authenticateApp(request, response, attributes, query);
            }
            return reject(response, "不支持的 deviceType: " + deviceType, null);
        } catch (RuntimeException e) {
            log.warn("StackChan WS 握手异常", e);
            return reject(response, "握手鉴权异常", null);
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需在握手完成后做额外处理
    }

    /** 固件侧：Device-Id + HMAC token，复用 StackChanDeviceAuth 的校验口径 */
    private boolean authenticateStackChan(ServerHttpRequest request, ServerHttpResponse response,
                                          Map<String, Object> attributes) {
        HttpServletRequest servletRequest = resolveServletRequest(request);
        if (servletRequest == null) {
            return reject(response, "设备鉴权需要 Servlet 运行环境", null);
        }
        String normalizedMac = StackChanWsCodec.normalizeMac(servletRequest.getHeader(HEADER_DEVICE_ID));
        if (normalizedMac == null) {
            return reject(response, "缺少或非法的 Device-Id", null);
        }
        String mac;
        try {
            // 端上可能发纯 12 hex（无分隔符），包一层把 Device-Id 归一化成小写冒号 MAC 再交给 P0 的鉴权组件
            mac = stackChanDeviceAuth.authenticate(new NormalizedDeviceIdRequest(servletRequest, normalizedMac));
        } catch (RuntimeException e) {
            return reject(response, e.getMessage(), normalizedMac);
        }
        if (!StringUtils.hasText(mac)) {
            return reject(response, "设备鉴权失败", normalizedMac);
        }
        attributes.put(ATTR_DEVICE_TYPE, DEVICE_TYPE_STACK_CHAN);
        attributes.put(ATTR_DEVICE_ID, mac);
        return true;
    }

    /** App 侧：Sa-Token 登录态 + 设备归属校验 */
    private boolean authenticateApp(ServerHttpRequest request, ServerHttpResponse response,
                                    Map<String, Object> attributes, Map<String, String> query) {
        // Device-Id 头是目标 StackChan MAC；缺头时兼容查询参数 deviceId 直接放 MAC 的旧约定
        String targetMac = StackChanWsCodec.normalizeMac(request.getHeaders().getFirst(HEADER_DEVICE_ID));
        if (targetMac == null) {
            targetMac = StackChanWsCodec.normalizeMac(query.get("deviceId"));
        }
        if (targetMac == null || !CommonUtils.isMacAddressValid(targetMac)) {
            return reject(response, "缺少或非法的 Device-Id", null);
        }

        String token = extractAppToken(request, query);
        if (!StringUtils.hasText(token)) {
            return reject(response, "缺少登录 token", targetMac);
        }
        Integer userId = resolveLoginUserId(token);
        if (userId == null) {
            return reject(response, "登录 token 无效或已过期", targetMac);
        }

        DeviceBO device;
        try {
            device = deviceService.getBO(targetMac);
        } catch (RuntimeException e) {
            log.warn("StackChan WS 查询设备失败, mac={}", targetMac, e);
            return reject(response, "查询设备失败", targetMac);
        }
        if (device == null) {
            return reject(response, "设备不存在或未绑定", targetMac);
        }
        if (!Objects.equals(device.getUserId(), userId)) {
            return reject(response, "设备不属于当前登录用户", targetMac);
        }

        String appDeviceId = query.get("deviceId");
        if (!StringUtils.hasText(appDeviceId)) {
            appDeviceId = targetMac;
        }
        attributes.put(ATTR_DEVICE_TYPE, DEVICE_TYPE_APP);
        attributes.put(ATTR_DEVICE_ID, targetMac);
        attributes.put(ATTR_APP_DEVICE_ID, appDeviceId);
        attributes.put(ATTR_USER_ID, userId);
        return true;
    }

    /** App 登录 token：Authorization（Bearer 可选）优先，其次 token 头，最后查询参数（浏览器无法设头时） */
    private static String extractAppToken(ServerHttpRequest request, Map<String, String> query) {
        String token = stripBearer(request.getHeaders().getFirst(HEADER_AUTHORIZATION));
        if (!StringUtils.hasText(token)) {
            token = stripBearer(request.getHeaders().getFirst(HEADER_TOKEN));
        }
        if (!StringUtils.hasText(token)) {
            token = stripBearer(query.get("token"));
        }
        return token;
    }

    /** Sa-Token 登录态解析：JWT Simple 模式下可直接从 token 反解 loginId，无需查会话 */
    private static Integer resolveLoginUserId(String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            if (loginId instanceof Number number) {
                return number.intValue();
            }
            return Integer.valueOf(String.valueOf(loginId).trim());
        } catch (RuntimeException e) {
            log.debug("StackChan WS App 登录态校验失败", e);
            return null;
        }
    }

    /**
     * 握手请求在 DispatcherServlet 内执行，正常情况下能拿到 ServletServerHttpRequest；
     * 单测里用 MockServerHttpRequest 时退回 RequestContextHolder。
     */
    private static HttpServletRequest resolveServletRequest(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest();
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static String stripBearer(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String token = value.trim();
        if (token.length() > BEARER_PREFIX.length()
                && token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token;
    }

    private static boolean reject(ServerHttpResponse response, String reason, String mac) {
        log.warn("StackChan WS 握手鉴权失败: {}{}", reason,
                StringUtils.hasText(mac) ? " - mac: " + mac : "");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    /** 与 Go 版 ghttp 的 query 读取等价的最小实现（WebSocket 握手阶段没有 Spring MVC 参数绑定） */
    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (!StringUtils.hasText(rawQuery)) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                log.debug("StackChan WS 握手 query 参数解码失败, pair={}", pair, e);
            }
        }
        return params;
    }

    /** 只覆写 Device-Id 头，把端上各种 MAC 写法归一化成小写冒号格式交给 StackChanDeviceAuth */
    private static final class NormalizedDeviceIdRequest extends HttpServletRequestWrapper {

        private final String deviceId;

        private NormalizedDeviceIdRequest(HttpServletRequest request, String deviceId) {
            super(request);
            this.deviceId = deviceId;
        }

        @Override
        public String getHeader(String name) {
            if (HEADER_DEVICE_ID.equalsIgnoreCase(name)) {
                return deviceId;
            }
            return super.getHeader(name);
        }
    }
}
