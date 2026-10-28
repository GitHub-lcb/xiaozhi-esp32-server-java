package com.xiaozhi.ai.homeassistant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 未配置时的行为比配置后更重要：没接 HA 必须明确报错，不能假装成功。
 */
class HomeAssistantClientTest {

    private HomeAssistantClient client() {
        return new HomeAssistantClient();
    }

    @Test
    void unavailableWithoutConfig() {
        HomeAssistantClient client = client();

        assertThat(client.available()).isFalse();
        assertThatThrownBy(() -> client.callService("homeassistant", "turn_on", "light.a"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("未配置 Home Assistant");
    }

    @Test
    void availableWhenBothConfigured() {
        HomeAssistantClient client = client();
        ReflectionTestUtils.setField(client, "baseUrl", "http://ha.local:8123");
        ReflectionTestUtils.setField(client, "token", "token-1");

        assertThat(client.available()).isTrue();
    }

    @Test
    void payloadCarriesEntityId() {
        assertThat(HomeAssistantClient.payload("light.living_room"))
            .isEqualTo("{\"entity_id\":\"light.living_room\"}");
        assertThat(HomeAssistantClient.payload("  ")).isEqualTo("{}");
    }

    @Test
    void blankDomainIsRejected() {
        HomeAssistantClient client = client();
        ReflectionTestUtils.setField(client, "baseUrl", "http://ha.local:8123");
        ReflectionTestUtils.setField(client, "token", "t");

        assertThatThrownBy(() -> client.callService("", "turn_on", "light.a"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}