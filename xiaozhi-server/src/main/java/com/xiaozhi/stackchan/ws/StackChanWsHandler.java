package com.xiaozhi.stackchan.ws;

import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * StackChan 伴侣 WebSocket 的连接生命周期与心跳调度。
 * <p>
 * 对应 Go 版 {@code web_socket.Handler}：握手鉴权由
 * {@link StackChanWsHandshakeInterceptor} 完成，本类负责连接建立/断开后的连接池维护、
 * 在线/离线通知、消息分发，以及 {@code socket_task.go} 里的 ping 与超时清理。
 * <p>
 * 心跳用二进制 0x10 ping（不是 WebSocket 控制帧）：设备回 0x11，App 回 0x11；
 * 每 90 秒没收到任何消息的连接会被关闭，避免半开连接长期占住连接池。
 */
@Slf4j
@Component
public class StackChanWsHandler extends TextWebSocketHandler {

    /** 设备重连后告知设备（对应 Go 版 "The equipment has been reconnected."） */
    private static final String DEVICE_RECONNECTED_TEXT = "The equipment has been reconnected.";

    @Resource
    private StackChanWsSessionRegistry sessionRegistry;

    @Resource
    private StackChanWsSender sender;

    @Resource
    private StackChanWsDeviceHandler deviceHandler;

    @Resource
    private StackChanWsAppHandler appHandler;

    /** 超过该时长没收到任何消息即判定连接失效（毫秒） */
    @Value("${stackchan.ws.client-expire-timeout-millis:90000}")
    private long clientExpireTimeoutMillis;

    // ==================== 连接生命周期 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceType = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_TYPE);
        String mac = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (!StringUtils.hasText(deviceType) || !StringUtils.hasText(mac)) {
            log.warn("StackChan WS 连接缺少握手属性，直接关闭, sessionId={}", session.getId());
            StackChanWsConnection.closeQuietly(session);
            return;
        }
        try {
            if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_STACK_CHAN.equals(deviceType)) {
                onDeviceConnected(session, mac);
            } else if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_APP.equals(deviceType)) {
                onAppConnected(session, mac);
            } else {
                log.warn("StackChan WS 未知 deviceType={}，直接关闭, sessionId={}", deviceType, session.getId());
                StackChanWsConnection.closeQuietly(session);
            }
        } catch (RuntimeException e) {
            log.warn("StackChan WS 连接建立后处理失败, mac={}", mac, e);
            StackChanWsConnection.closeQuietly(session);
        }
    }

    /**
     * 设备接入：同 MAC 单连接，重连复用连接对象以保留订阅/通话状态，并通知 App 上线。
     * <p>
     * 不按「是否重连」分支判断，而是按「连接上是否已经有订阅/通话状态」补发 OnCamera/OnAudio：
     * 这样设备短暂断线（连接对象仍留在池里、会话已解绑）后重连也能恢复推流。
     */
    private void onDeviceConnected(WebSocketSession session, String mac) {
        boolean existed = sessionRegistry.getDevice(mac) != null;
        StackChanDeviceConnection device = sessionRegistry.registerDevice(mac);
        WebSocketSession previous = device.attachSession(session);
        if (previous != null) {
            StackChanWsConnection.closeQuietly(previous);
        }
        if (existed) {
            log.info("StackChan 设备重连, mac={}", mac);
        }
        if (device.getCallAppConnection() != null) {
            sender.sendToDevice(device, StackChanWsMessageType.TEXT_MESSAGE,
                    DEVICE_RECONNECTED_TEXT.getBytes(StandardCharsets.UTF_8));
        }
        if (device.getCameraSubscriberCount() > 0) {
            sender.sendToDevice(device, StackChanWsMessageType.ON_CAMERA, null);
        }
        if (device.getAudioSubscriberCount() > 0) {
            sender.sendToDevice(device, StackChanWsMessageType.ON_AUDIO, null);
        }
        // 无论新连还是重连，都通知该 MAC 下的 App 设备已在线（对应 Go 版 Handler 末尾的 send Online）
        sender.notifyDeviceOnline(mac);
        log.info("StackChan 设备已连接, mac={}, 设备连接数={}, App连接数={}",
                mac, sessionRegistry.deviceCount(), sessionRegistry.appCount());
    }

    /**
     * App 接入：同 MAC 多连接；同一 App 实例重连时复用原来的连接对象（订阅/通话状态跟着对象走），
     * 只替换底层会话，并告知它设备当前在线状态。
     */
    private void onAppConnected(WebSocketSession session, String mac) {
        String appDeviceId = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_APP_DEVICE_ID);
        if (!StringUtils.hasText(appDeviceId)) {
            appDeviceId = session.getId();
        }
        Integer userId = session.getAttributes().get(StackChanWsHandshakeInterceptor.ATTR_USER_ID)
                instanceof Integer id ? id : null;

        List<StackChanAppConnection> sameInstance = sessionRegistry.findAppsByAppDeviceId(mac, appDeviceId);
        StackChanAppConnection app = sameInstance.isEmpty() ? null : sameInstance.get(0);
        // 同实例异常残留的多余连接：踢掉，避免同一台手机出现多条在线连接
        for (int i = 1; i < sameInstance.size(); i++) {
            StackChanAppConnection duplicate = sameInstance.get(i);
            if (sessionRegistry.removeApp(duplicate)) {
                detachAppFromDevices(duplicate);
                duplicate.close();
            }
        }
        boolean created = app == null;
        if (created) {
            app = new StackChanAppConnection(mac, appDeviceId, userId);
        }
        WebSocketSession previous = app.attachSession(session);
        if (previous != null) {
            StackChanWsConnection.closeQuietly(previous);
        }
        if (created) {
            sessionRegistry.addApp(app);
        }

        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device == null || !device.isOnline()) {
            sender.notifyAppDeviceOffline(app);
        } else {
            sender.notifyAppDeviceOnline(app);
        }
        log.info("StackChan App 已连接, mac={}, appDeviceId={}, userId={}, 复用连接={}, App连接数={}",
                mac, appDeviceId, userId, !created, sessionRegistry.appCount());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceType = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_TYPE);
        String mac = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_ID);
        try {
            if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_STACK_CHAN.equals(deviceType) && StringUtils.hasText(mac)) {
                onDeviceDisconnected(session, mac, status);
            } else if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_APP.equals(deviceType) && StringUtils.hasText(mac)) {
                onAppDisconnected(session, mac, status);
            }
        } catch (RuntimeException e) {
            log.warn("StackChan WS 连接关闭清理失败, mac={}", mac, e);
        }
    }

    /**
     * 设备断开：只有「当前会话」的关闭才处理（重连时旧会话的关闭回调必须忽略）。
     * <p>
     * 通知 App 离线，但连接对象先留在池里（会话已解绑），等超时清理再移除——这样设备
     * 短时间重连能复用原来的订阅/通话状态，对应 Go 版「断线只置空 conn、由超时任务清池」。
     */
    private void onDeviceDisconnected(WebSocketSession session, String mac, CloseStatus status) {
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device == null || !device.detachSessionIfCurrent(session.getId())) {
            return;
        }
        sender.notifyDeviceOffline(mac);
        log.info("StackChan 设备已断开（保留订阅状态等待重连或超时清理）, mac={}, status={}", mac, status);
    }

    /**
     * App 断开：解开当前会话但保留连接对象与订阅关系，等待重连复用；
     * 若一直不重连，由超时清理统一摘除订阅并回收。
     */
    private void onAppDisconnected(WebSocketSession session, String mac, CloseStatus status) {
        StackChanAppConnection app = sessionRegistry.findAppBySession(mac, session.getId());
        if (app == null || !app.detachSessionIfCurrent(session.getId())) {
            return;
        }
        log.info("StackChan App 已断开（保留订阅状态等待重连或超时清理）, mac={}, appDeviceId={}, status={}",
                mac, app.getAppDeviceId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("StackChan WS 传输异常, sessionId={}", session.getId(), exception);
        StackChanWsConnection.closeQuietly(session);
    }

    // ==================== 消息分发 ====================

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String deviceType = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_TYPE);
        try {
            if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_STACK_CHAN.equals(deviceType)) {
                StackChanDeviceConnection device = resolveDevice(session);
                if (device != null) {
                    device.touch();
                    deviceHandler.handleText(device, message.getPayload());
                }
            } else if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_APP.equals(deviceType)) {
                StackChanAppConnection app = resolveApp(session);
                if (app != null) {
                    app.touch();
                    appHandler.handleText(app, message.getPayload());
                }
            }
        } catch (RuntimeException e) {
            log.warn("处理 StackChan WS 文本消息失败, sessionId={}", session.getId(), e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] frame;
        try {
            frame = toByteArray(message.getPayload());
        } catch (RuntimeException e) {
            log.warn("读取 StackChan WS 二进制消息失败, sessionId={}", session.getId(), e);
            return;
        }
        StackChanWsCodec.ParsedFrame parsed = StackChanWsCodec.parse(frame);
        if (parsed == null) {
            log.warn("StackChan WS 二进制帧非法（长度不足或头部长度不一致），已丢弃, sessionId={}, size={}",
                    session.getId(), frame.length);
            return;
        }
        String deviceType = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_TYPE);
        try {
            if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_STACK_CHAN.equals(deviceType)) {
                StackChanDeviceConnection device = resolveDevice(session);
                if (device != null) {
                    device.touch();
                    deviceHandler.handle(device, frame, parsed);
                }
            } else if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_APP.equals(deviceType)) {
                StackChanAppConnection app = resolveApp(session);
                if (app != null) {
                    app.touch();
                    appHandler.handle(app, frame, parsed);
                }
            }
        } catch (RuntimeException e) {
            log.warn("处理 StackChan WS 二进制消息失败, sessionId={}, msgType={}",
                    session.getId(), parsed.msgType(), e);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        // WebSocket 控制帧 pong 也当作存活信号；协议层的 0x11 走二进制分支
        touchConnection(session);
    }

    private void touchConnection(WebSocketSession session) {
        String deviceType = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_TYPE);
        if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_STACK_CHAN.equals(deviceType)) {
            StackChanDeviceConnection device = resolveDevice(session);
            if (device != null) {
                device.touch();
            }
        } else if (StackChanWsHandshakeInterceptor.DEVICE_TYPE_APP.equals(deviceType)) {
            StackChanAppConnection app = resolveApp(session);
            if (app != null) {
                app.touch();
            }
        }
    }

    // ==================== 心跳与超时清理 ====================

    /** 每 5 秒给所有连接发一次二进制 ping（Go 版 StartPingTime） */
    @Scheduled(fixedRateString = "${stackchan.ws.ping-interval-millis:5000}")
    public void pingAllConnections() {
        byte[] pingFrame = StackChanWsCodec.encode(StackChanWsMessageType.PING, null);
        for (StackChanDeviceConnection device : sessionRegistry.allDevices()) {
            try {
                device.sendBinary(pingFrame);
            } catch (RuntimeException e) {
                log.debug("StackChan 设备心跳发送失败, mac={}", device.getMac(), e);
            }
        }
        for (StackChanAppConnection app : sessionRegistry.allApps()) {
            try {
                app.sendBinary(pingFrame);
            } catch (RuntimeException e) {
                log.debug("StackChan App 心跳发送失败, mac={}", app.getMac(), e);
            }
        }
    }

    /** 每 15 秒清理超过 90 秒无消息的连接（Go 版 CheckExpiredLinks） */
    @Scheduled(fixedRateString = "${stackchan.ws.expire-check-interval-millis:15000}")
    public void evictExpiredConnections() {
        long now = DateUtils.millis();
        evictExpiredApps(now);
        evictExpiredDevices(now);
    }

    private void evictExpiredApps(long now) {
        for (StackChanAppConnection app : sessionRegistry.allApps()) {
            if (now - app.getLastActiveMillis() <= clientExpireTimeoutMillis) {
                continue;
            }
            WebSocketSession session = app.detachSession();
            if (!sessionRegistry.removeApp(app)) {
                continue;
            }
            detachAppFromDevices(app);
            StackChanWsConnection.closeQuietly(session);
            if (app.getSession() != null) {
                // 极窄竞态：清理与重连并发，移除后又有新会话挂上，放回池里继续用
                sessionRegistry.addApp(app);
                log.info("超时清理与重连并发，保留 App 连接, mac={}, appDeviceId={}",
                        app.getMac(), app.getAppDeviceId());
                continue;
            }
            log.info("清理超时 App 连接, mac={}, appDeviceId={}", app.getMac(), app.getAppDeviceId());
        }
    }

    private void evictExpiredDevices(long now) {
        for (StackChanDeviceConnection device : sessionRegistry.allDevices()) {
            if (now - device.getLastActiveMillis() <= clientExpireTimeoutMillis) {
                continue;
            }
            // 正常断开时已经通知过离线，这里只对「超时时仍在线」（半开连接）的设备补一次离线通知
            boolean wasOnline = device.isOnline();
            String mac = device.getMac();
            WebSocketSession session = device.detachSession();
            if (!sessionRegistry.removeDevice(mac, device)) {
                continue;
            }
            StackChanWsConnection.closeQuietly(session);
            if (device.getSession() != null) {
                // 极窄竞态：清理与重连并发，移除后又有新会话挂上，放回池里继续用
                sessionRegistry.restoreDevice(device);
                log.info("超时清理与重连并发，保留 StackChan 连接, mac={}", mac);
                continue;
            }
            if (wasOnline) {
                sender.notifyDeviceOffline(mac);
            }
            log.info("清理超时 StackChan 连接, mac={}, 清理前在线={}", mac, wasOnline);
        }
    }

    // ==================== 内部工具 ====================

    /** App 断开/超时后，把它从所有设备的订阅列表、通话对端、瞄准拍照引用里摘掉 */
    private void detachAppFromDevices(StackChanAppConnection app) {
        for (StackChanDeviceConnection device : sessionRegistry.allDevices()) {
            if (device.getCallAppConnection() == app) {
                device.setCallAppConnection(null);
            }
            device.clearAimedTakePhotoAppConnection(app);

            boolean cameraRemoved = device.removeCameraSubscriber(app);
            if (cameraRemoved && device.getCameraSubscriberCount() == 0) {
                sender.sendToDevice(device, StackChanWsMessageType.OFF_CAMERA, null);
            }
            boolean audioRemoved = device.removeAudioSubscriber(app);
            if (audioRemoved && device.getAudioSubscriberCount() == 0) {
                sender.sendToDevice(device, StackChanWsMessageType.OFF_AUDIO, null);
            }
        }
    }

    /** 按会话解析设备连接；只有「当前会话」才返回，重连后的旧会话消息直接丢弃 */
    private StackChanDeviceConnection resolveDevice(WebSocketSession session) {
        String mac = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (!StringUtils.hasText(mac)) {
            return null;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device == null || !device.isCurrentSession(session.getId())) {
            return null;
        }
        return device;
    }

    /** 按会话解析 App 连接 */
    private StackChanAppConnection resolveApp(WebSocketSession session) {
        String mac = stringAttribute(session, StackChanWsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (!StringUtils.hasText(mac)) {
            return null;
        }
        return sessionRegistry.findAppBySession(mac, session.getId());
    }

    private static String stringAttribute(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value instanceof String text ? text : null;
    }

    private static byte[] toByteArray(ByteBuffer payload) {
        if (payload == null) {
            return new byte[0];
        }
        ByteBuffer view = payload.asReadOnlyBuffer();
        byte[] data = new byte[view.remaining()];
        view.get(data);
        return data;
    }
}
