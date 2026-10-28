package com.xiaozhi.communication.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MQTT 设备通道配置。
 * <p>
 * 设备侧拿到的是 OTA 响应里的 {@code mqtt} 块，服务端拿的是这一份：
 * {@code brokerUrl} 是本进程连 broker 的地址，{@code publicEndpoint} 是下发给设备的地址
 * （设备与服务器看到的 broker 地址经常不一样，比如 Docker 内网 vs 局域网）。
 * <p>
 * 主题模板同时用于上行与下行，{@code {deviceId}} 会被替换成设备ID；
 * 与固件约定一致时保持默认值 {@code devices/p2p/{deviceId}}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaozhi.mqtt")
public class MqttProperties {

    public static final String DEVICE_ID_PLACEHOLDER = "{deviceId}";

    /** 是否启用 MQTT 通道。关闭时 dialogue 不连 broker，OTA 也不下发 mqtt 配置 */
    private boolean enabled = false;

    /** 服务端连 broker 的地址，如 tcp://192.168.1.10:1883 */
    private String brokerUrl = "tcp://127.0.0.1:1883";

    /** 下发给设备的 broker 地址，留空则复用 brokerUrl */
    private String publicEndpoint;

    private String clientId = "xiaozhi-dialogue";

    private String username = "";

    private String password = "";

    /** 上下行主题模板，{deviceId} 占位 */
    private String topicTemplate = "devices/p2p/{deviceId}";

    private int qos = 1;

    private int keepAliveSeconds = 60;

    /** 超过这个时间没有任何上行消息，巡检把通道置为离线 */
    private int presenceTtlSeconds = 120;

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(brokerUrl);
    }

    /** 设备主题：把模板里的占位符换掉 */
    public String topicFor(String deviceId) {
        return topicTemplate.replace(DEVICE_ID_PLACEHOLDER, deviceId);
    }

    /** 订阅过滤器：占位符换成 MQTT 的单层通配符 */
    public String subscribeFilter() {
        return topicTemplate.replace(DEVICE_ID_PLACEHOLDER, "+");
    }

    /** 从实际主题反解设备ID，主题不符合模板时返回 null */
    public String deviceIdOf(String topic) {
        if (!StringUtils.hasText(topic) || !StringUtils.hasText(topicTemplate)) {
            return null;
        }
        String[] templateParts = topicTemplate.split("/");
        String[] topicParts = topic.split("/");
        if (templateParts.length != topicParts.length) {
            return null;
        }
        String deviceId = null;
        for (int i = 0; i < templateParts.length; i++) {
            if (DEVICE_ID_PLACEHOLDER.equals(templateParts[i])) {
                if (!StringUtils.hasText(topicParts[i])) {
                    return null;
                }
                deviceId = topicParts[i];
            } else if (!templateParts[i].equals(topicParts[i])) {
                return null;
            }
        }
        return deviceId;
    }

    /** 设备侧连接地址：优先用单独配置的对外地址 */
    public String getPublicEndpointOrDefault() {
        return StringUtils.hasText(publicEndpoint) ? publicEndpoint : brokerUrl;
    }
}
