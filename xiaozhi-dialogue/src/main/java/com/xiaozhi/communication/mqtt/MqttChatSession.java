package com.xiaozhi.communication.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 通道上的设备会话。
 * <p>
 * 与 WebSocket 会话共用同一套对话管线（{@link ChatSession}），差别只在传输：
 * 文本消息走 JSON 主题，音频帧按 MQTT 协议约定用 base64 塞进 JSON（WebSocket 上是二进制帧）。
 */
@Slf4j
public class MqttChatSession extends ChatSession {

    /** 下发通道：由 MQTT 桥实现，测试里注入假的发布器 */
    @FunctionalInterface
    public interface Publisher {
        void publish(String topic, byte[] payload, int qos, boolean retained);
    }

    private final String deviceId;
    private final String topic;
    private final int qos;
    private final Publisher publisher;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public MqttChatSession(String deviceId, String topic, int qos, Publisher publisher) {
        super("mqtt:" + deviceId);
        this.deviceId = deviceId;
        this.topic = topic;
        this.qos = qos;
        this.publisher = publisher;
    }

    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public boolean isAudioChannelOpen() {
        return open.get();
    }

    @Override
    public void close() {
        open.set(false);
    }

    @Override
    public void sendTextMessage(String message) {
        if (!isOpen()) {
            log.debug("MQTT 会话已关闭，丢弃下行文本 - deviceId={}", deviceId);
            return;
        }
        publisher.publish(topic, message.getBytes(StandardCharsets.UTF_8), qos, false);
    }

    /**
     * 二进制帧（opus）在 MQTT 上按协议用 base64 放进 JSON，时间戳原样带上，服务端 AEC 靠它对齐。
     */
    @Override
    public void sendBinaryMessage(byte[] message, long timestamp) {
        if (!isOpen() || message == null || message.length == 0) {
            return;
        }
        ObjectNode node = JsonUtil.OBJECT_MAPPER.createObjectNode();
        node.put("type", "audio");
        node.put("timestamp", timestamp);
        node.put("audio", Base64.getEncoder().encodeToString(message));
        publisher.publish(topic, node.toString().getBytes(StandardCharsets.UTF_8), qos, false);
    }
}
