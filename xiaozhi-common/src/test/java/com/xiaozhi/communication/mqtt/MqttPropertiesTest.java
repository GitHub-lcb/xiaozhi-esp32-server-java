package com.xiaozhi.communication.mqtt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主题模板是服务端与固件之间的硬约定：模板换了，设备的上行会被当成「非设备主题」丢掉，
 * 而且不会有任何报错，所以把占位符替换与反解都钉住。
 */
class MqttPropertiesTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Test
    void topicFollowsTheTemplate() {
        MqttProperties properties = new MqttProperties();
        properties.setTopicTemplate("devices/p2p/{deviceId}");

        assertThat(properties.topicFor(DEVICE_ID)).isEqualTo("devices/p2p/" + DEVICE_ID);
        assertThat(properties.subscribeFilter()).isEqualTo("devices/p2p/+");
    }

    @Test
    void deviceIdIsRecoveredFromTopic() {
        MqttProperties properties = new MqttProperties();

        assertThat(properties.deviceIdOf("devices/p2p/" + DEVICE_ID)).isEqualTo(DEVICE_ID);
    }

    @Test
    void topicFromAnotherNamespaceIsIgnored() {
        MqttProperties properties = new MqttProperties();

        assertThat(properties.deviceIdOf("other/p2p/" + DEVICE_ID)).isNull();
        assertThat(properties.deviceIdOf("devices/p2p/")).isNull();
        assertThat(properties.deviceIdOf(null)).isNull();
    }

    @Test
    void customTemplateIsSupported() {
        MqttProperties properties = new MqttProperties();
        properties.setTopicTemplate("xiaozhi/{deviceId}/up");

        assertThat(properties.topicFor(DEVICE_ID)).isEqualTo("xiaozhi/" + DEVICE_ID + "/up");
        assertThat(properties.deviceIdOf("xiaozhi/" + DEVICE_ID + "/up")).isEqualTo(DEVICE_ID);
    }

    @Test
    void publicEndpointFallsBackToBrokerUrl() {
        MqttProperties properties = new MqttProperties();
        properties.setBrokerUrl("tcp://10.0.0.2:1883");

        assertThat(properties.getPublicEndpointOrDefault()).isEqualTo("tcp://10.0.0.2:1883");

        properties.setPublicEndpoint("tcp://192.168.1.10:1883");
        assertThat(properties.getPublicEndpointOrDefault()).isEqualTo("tcp://192.168.1.10:1883");
    }

    @Test
    void disabledWhenNoBrokerConfigured() {
        MqttProperties properties = new MqttProperties();
        properties.setEnabled(true);
        properties.setBrokerUrl("  ");

        assertThat(properties.isEnabled()).isFalse();
    }
}
