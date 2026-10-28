package com.xiaozhi.communication.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.MessageHandler;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.domain.GoodbyeMessage;
import com.xiaozhi.communication.domain.Message;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import lombok.extern.slf4j.Slf4j;

/**
 * MQTT 设备通道：连 broker、订阅设备主题、把上行消息喂给同一套对话管线。
 * <p>
 * 设备第一次发消息时建立 {@link MqttChatSession}，之后的 hello/listen/iot/mcp 与 WebSocket 走的是同一个
 * {@link MessageHandler}，所以对话、打断、TTS 下发不需要为 MQTT 单独实现一遍。
 * <p>
 * 只做传输：不鉴权（设备鉴权在 OTA 下发 token 时已确定，MQTT 侧靠 broker 的账号密码），
 * 也不改设备表，只维护通道在线状态。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "xiaozhi.mqtt", name = "enabled", havingValue = "true")
public class MqttBridge implements MqttCallbackExtended {

    private static final String MSG_TYPE_AUDIO = "audio";

    private final MqttProperties properties;
    private final MessageHandler messageHandler;
    private final SessionManager sessionManager;
    private final DeviceChannelService deviceChannelService;

    private MqttAsyncClient client;

    public MqttBridge(MqttProperties properties, MessageHandler messageHandler, SessionManager sessionManager,
                      DeviceChannelService deviceChannelService) {
        this.properties = properties;
        this.messageHandler = messageHandler;
        this.sessionManager = sessionManager;
        this.deviceChannelService = deviceChannelService;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("MQTT 通道未启用（xiaozhi.mqtt.enabled=false），跳过连接");
            return;
        }
        try {
            client = new MqttAsyncClient(properties.getBrokerUrl(), properties.getClientId(),
                new MemoryPersistence());
            client.setCallback(this);
            client.connect(buildOptions()).waitForCompletion(10_000);
            log.info("MQTT 通道已连接 broker={}，订阅 {}",
                properties.getBrokerUrl(), properties.subscribeFilter());
        } catch (MqttException | RuntimeException e) {
            // 连不上 broker 不能把 dialogue 拖死：WebSocket 通道仍然可用，等 broker 恢复后重连
            log.error("MQTT 通道连接失败 broker={}，设备暂时只能走 WebSocket", properties.getBrokerUrl(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion(3_000);
            }
            client.close();
        } catch (MqttException e) {
            log.warn("关闭 MQTT 客户端失败", e);
        }
    }

    private MqttConnectOptions buildOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setKeepAliveInterval(properties.getKeepAliveSeconds());
        options.setConnectionTimeout(10);
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            options.setUserName(properties.getUsername());
            options.setPassword(properties.getPassword().toCharArray());
        }
        return options;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe(properties.subscribeFilter(), properties.getQos());
            log.info("MQTT 通道{}，已订阅 {}", reconnect ? "重连成功" : "连接成功", properties.subscribeFilter());
        } catch (MqttException e) {
            log.error("MQTT 订阅失败 topic={}", properties.subscribeFilter(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT 连接断开，等待自动重连", cause);
        // 断线期间设备实际不可达，先把 MQTT 会话标离线，重连后设备下一条消息会重新建立
        sessionManager.getAllSessions().stream()
            .filter(MqttChatSession.class::isInstance)
            .map(MqttChatSession.class::cast)
            .filter(MqttChatSession::isOpen)
            .forEach(session -> {
                session.close();
                deviceChannelService.markOffline(session.getDeviceId());
            });
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String deviceId = properties.deviceIdOf(topic);
        if (deviceId == null) {
            log.debug("忽略非设备主题的消息 topic={}", topic);
            return;
        }
        ChatSession session = sessionManager.getSessionByDeviceId(deviceId);
        if (session == null || !(session instanceof MqttChatSession)) {
            session = openSession(deviceId);
        }
        if (session == null) {
            return;
        }
        deviceChannelService.touch(deviceId);
        dispatch(session, new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 无需处理：下行丢不丢由 broker 的 QoS 保证
    }

    private ChatSession openSession(String deviceId) {
        MqttChatSession session = new MqttChatSession(deviceId, properties.topicFor(deviceId),
            properties.getQos(), this::publish);
        try {
            messageHandler.afterConnection(session, deviceId);
        } catch (RuntimeException e) {
            log.error("MQTT 建立会话失败 deviceId={}", deviceId, e);
            session.close();
            return null;
        }
        deviceChannelService.markOnline(deviceId, DeviceChannelBO.CHANNEL_MQTT, properties.getClientId());
        log.info("MQTT 设备通道建立 deviceId={} sessionId={}", deviceId, session.getSessionId());
        return session;
    }

    /**
     * 音频帧在 MQTT 上是 base64 的 JSON，其余消息与 WebSocket 的 JSON 完全一致。
     */
    private void dispatch(ChatSession session, String payload) {
        try {
            JsonNode node = JsonUtil.OBJECT_MAPPER.readTree(payload);
            String type = node.path("type").asText("");
            if (MSG_TYPE_AUDIO.equals(type)) {
                byte[] opus = Base64.getDecoder().decode(node.path("audio").asText(""));
                long timestamp = node.path("timestamp").asLong(0L);
                messageHandler.handleBinaryMessage(session.getSessionId(), opus, timestamp);
                return;
            }
            Message msg = JsonUtil.fromJson(payload, Message.class);
            messageHandler.handleMessage(msg, session.getSessionId());
            if (msg instanceof GoodbyeMessage && session instanceof MqttChatSession mqttSession) {
                session.close();
                deviceChannelService.markOffline(mqttSession.getDeviceId());
            }
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("MQTT 上行消息解析失败，已丢弃 payload={}", abbreviate(payload), e);
        }
    }

    private void publish(String topic, byte[] payload, int qos, boolean retained) {
        MqttAsyncClient current = client;
        if (current == null || !current.isConnected()) {
            log.debug("MQTT 未连接，丢弃下行 topic={}", topic);
            return;
        }
        try {
            current.publish(topic, payload, qos, retained);
        } catch (MqttException e) {
            log.warn("MQTT 下行发送失败 topic={}", topic, e);
        }
    }

    private String abbreviate(String payload) {
        return payload != null && payload.length() > 200 ? payload.substring(0, 200) + "..." : payload;
    }
}
