package com.xiaozhi.stackchan.ws;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * StackChan 伴侣 WebSocket 的注册与容器参数。
 * <p>
 * 端点：{@code /api/stackChan/ws?deviceType=StackChan|App}，由 xiaozhi-server（8091）提供。
 * 固件把 {@code {STACKCHAN_SERVER_URL}}（形如 {@code https://host/api}）当前缀拼
 * {@code /stackChan/ws}，App 直接连 {@code wss://host/api/stackChan/ws}。
 * <p>
 * server 模块此前没有任何 WebSocket 容器配置（dialogue 模块的 WebSocketConfig 只服务于
 * dialogue 进程），Tomcat 的天然消息缓冲默认只有 8KB，摄像头 JPEG 帧会直接超过上限被断开，
 * 因此这里必须显式提高 {@code maxBinaryMessageBufferSize}。
 */
@Slf4j
@Configuration
@EnableWebSocket
public class StackChanWsConfig implements WebSocketConfigurer {

    /** StackChan 固件与 App 共用的伴侣 WebSocket 路径 */
    public static final String WS_PATH = "/api/stackChan/ws";

    /** JPEG 帧可能远超 Tomcat 默认 8KB，按 1MB 放开 */
    @Value("${stackchan.ws.max-binary-message-buffer-size:1048576}")
    private int maxBinaryMessageBufferSize;

    /** 端上只发小 JSON 文本（hello 等），64KB 足够 */
    @Value("${stackchan.ws.max-text-message-buffer-size:65536}")
    private int maxTextMessageBufferSize;

    /** 容器空闲超时交给业务心跳（90s 清理）判定；容器层放宽到 10 分钟兜底 */
    @Value("${stackchan.ws.max-session-idle-timeout:600000}")
    private long maxSessionIdleTimeout;

    @Value("${stackchan.ws.async-send-timeout:5000}")
    private long asyncSendTimeout;

    @Resource
    private StackChanWsHandler stackChanWsHandler;

    @Resource
    private StackChanWsHandshakeInterceptor stackChanWsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stackChanWsHandler, WS_PATH)
                .addInterceptors(stackChanWsHandshakeInterceptor)
                // 固件/App/浏览器都可能接入，跨域交由握手鉴权兜底
                .setAllowedOrigins("*");
        log.info("StackChan 伴侣 WebSocket 已注册: {}", WS_PATH);
    }

    @Bean
    public ServletServerContainerFactoryBean createStackChanWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(maxTextMessageBufferSize);
        container.setMaxBinaryMessageBufferSize(maxBinaryMessageBufferSize);
        container.setMaxSessionIdleTimeout(maxSessionIdleTimeout);
        container.setAsyncSendTimeout(asyncSendTimeout);
        return container;
    }
}
