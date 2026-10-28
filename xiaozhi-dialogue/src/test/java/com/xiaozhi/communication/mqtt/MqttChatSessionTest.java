package com.xiaozhi.communication.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaozhi.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MQTT 下行的两种形态：控制消息是原样 JSON，音频帧按协议约定 base64 塞进 JSON 并带时间戳。
 * 编码错了设备只会「没声音」，不会报错，所以这里逐字段钉住。
 */
class MqttChatSessionTest {

    private final List<byte[]> published = new ArrayList<>();
    private final List<String> topics = new ArrayList<>();

    private MqttChatSession newSession() {
        return new MqttChatSession("aa:bb:cc:dd:ee:ff", "devices/p2p/aa:bb:cc:dd:ee:ff", 1,
            (topic, payload, qos, retained) -> {
                topics.add(topic);
                published.add(payload);
            });
    }

    @Test
    void textMessageIsPublishedAsIs() {
        MqttChatSession session = newSession();

        session.sendTextMessage("{\"type\":\"tts\",\"state\":\"start\"}");

        assertThat(topics).containsExactly("devices/p2p/aa:bb:cc:dd:ee:ff");
        assertThat(new String(published.get(0), StandardCharsets.UTF_8))
            .isEqualTo("{\"type\":\"tts\",\"state\":\"start\"}");
    }

    @Test
    void binaryAudioIsBase64EncodedWithTimestamp() throws Exception {
        MqttChatSession session = newSession();
        byte[] opus = new byte[]{1, 2, 3, 4};

        session.sendBinaryMessage(opus, 12345L);

        JsonNode node = JsonUtil.OBJECT_MAPPER.readTree(new String(published.get(0), StandardCharsets.UTF_8));
        assertThat(node.path("type").asText()).isEqualTo("audio");
        assertThat(node.path("timestamp").asLong()).isEqualTo(12345L);
        assertThat(Base64.getDecoder().decode(node.path("audio").asText())).isEqualTo(opus);
    }

    @Test
    void closedSessionDropsEverything() {
        MqttChatSession session = newSession();
        session.close();

        session.sendTextMessage("{}");
        session.sendBinaryMessage(new byte[]{1}, 1L);

        assertThat(published).isEmpty();
        assertThat(session.isOpen()).isFalse();
        assertThat(session.isAudioChannelOpen()).isFalse();
    }

    @Test
    void sessionIdIsStablePerDevice() {
        MqttChatSession first = newSession();
        MqttChatSession second = newSession();

        assertThat(first.getSessionId()).isEqualTo(second.getSessionId()).startsWith("mqtt:");
    }
}
