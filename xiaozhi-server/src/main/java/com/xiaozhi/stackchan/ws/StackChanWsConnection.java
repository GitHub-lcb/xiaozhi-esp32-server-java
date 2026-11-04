package com.xiaozhi.stackchan.ws;

import com.xiaozhi.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;

/**
 * 伴侣 WebSocket 单条连接的公共状态与发送能力。
 * <p>
 * 原始会话一律包一层 {@link ConcurrentWebSocketSessionDecorator}（发送超时 5s、缓冲 512KB、
 * 溢出 DROP），保证业务线程并发发送时不会被底层 JSR-356 会话拒绝，也不会因为某个慢客户端
 * 把整个服务的发送线程阻塞住。
 * <p>
 * {@code lastActiveMillis} 在每次收到消息时刷新，心跳调度按它判定连接是否超时。
 */
@Slf4j
public abstract class StackChanWsConnection {

    /** 单条消息发送超时（毫秒） */
    public static final int SEND_TIME_LIMIT_MILLIS = 5_000;

    /** 发送缓冲上限（字节）：超限按 DROP 策略丢弃新消息，不关闭连接 */
    public static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

    private final String mac;
    private volatile WebSocketSession session;
    private volatile String sessionId;
    private volatile long lastActiveMillis;

    protected StackChanWsConnection(String mac) {
        this.mac = mac;
        this.lastActiveMillis = DateUtils.millis();
    }

    /** 连接归属的 StackChan 小写冒号 MAC */
    public String getMac() {
        return mac;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }

    /** 当前是否有活跃连接（会话可能已断开但还没被清理，发送前用这个判断） */
    public boolean isOnline() {
        WebSocketSession current = session;
        return current != null && current.isOpen();
    }

    /** 收到的 WebSocketSession 是否仍是本连接当前使用的会话（重连后旧会话的回调会被忽略） */
    public boolean isCurrentSession(String candidateSessionId) {
        return candidateSessionId != null && candidateSessionId.equals(sessionId);
    }

    /**
     * 挂载（或替换）底层会话，返回被替换掉的旧会话；调用方负责关闭旧会话，避免重连后两条连接并存。
     * 与 {@link #detachSessionIfCurrent(String)} 用同一把对象锁，避免「新连接刚挂上就被旧连接的
     * 关闭回调解绑」的竞态。
     */
    public synchronized WebSocketSession attachSession(WebSocketSession rawSession) {
        WebSocketSession previous = this.session;
        this.session = new ConcurrentWebSocketSessionDecorator(rawSession, SEND_TIME_LIMIT_MILLIS,
                SEND_BUFFER_LIMIT_BYTES, ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
        this.sessionId = rawSession.getId();
        touch();
        return previous;
    }

    /** 解绑底层会话并返回解绑前的会话（用于关闭超时/被替换的连接） */
    public synchronized WebSocketSession detachSession() {
        WebSocketSession current = this.session;
        this.session = null;
        this.sessionId = null;
        return current;
    }

    /**
     * 仅当当前仍是给定的那条会话时才解绑。
     * <p>
     * 关闭回调执行时可能设备/App 已经完成重连（同一连接对象挂上了新会话），此时必须忽略这条
     * 迟到的旧会话关闭事件，否则会把刚建立的新会话解绑掉。
     *
     * @return 是否真的解绑了当前会话
     */
    public synchronized boolean detachSessionIfCurrent(String candidateSessionId) {
        if (candidateSessionId != null && candidateSessionId.equals(sessionId)) {
            session = null;
            sessionId = null;
            return true;
        }
        return false;
    }

    public void touch() {
        this.lastActiveMillis = DateUtils.millis();
    }

    public long getLastActiveMillis() {
        return lastActiveMillis;
    }

    /** 发送已编码的二进制帧；连接已断开时静默忽略 */
    public void sendBinary(byte[] frame) {
        WebSocketSession current = session;
        if (current == null) {
            return;
        }
        try {
            current.sendMessage(new BinaryMessage(frame));
        } catch (IOException | RuntimeException e) {
            log.debug("StackChan WS 发送二进制消息失败, mac={}", mac, e);
        }
    }

    /** 发送文本帧；连接已断开时静默忽略 */
    public void sendText(String text) {
        WebSocketSession current = session;
        if (current == null || text == null) {
            return;
        }
        try {
            current.sendMessage(new TextMessage(text));
        } catch (IOException | RuntimeException e) {
            log.debug("StackChan WS 发送文本消息失败, mac={}", mac, e);
        }
    }

    /** 关闭当前会话（先解绑，避免关闭回调再次触发清理） */
    public void close() {
        closeQuietly(detachSession());
    }

    protected static void closeQuietly(WebSocketSession target) {
        if (target == null) {
            return;
        }
        try {
            target.close(CloseStatus.GOING_AWAY);
        } catch (IOException | RuntimeException e) {
            log.debug("StackChan WS 关闭会话失败, sessionId={}", target.getId(), e);
        }
    }
}
