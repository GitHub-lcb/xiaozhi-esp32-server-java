package com.xiaozhi.communication.mqtt;

import com.xiaozhi.channel.service.DeviceChannelService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * MQTT 通道的在线巡检。
 * <p>
 * MQTT 不像 WebSocket 有明确的关闭事件：设备断电、断网、换 WiFi 都可能只表现为「不再发消息」。
 * 桥在收到上行消息时续期 lastSeenTime，这里定期把超过 TTL 没消息的通道置为离线，
 * 避免主动推送往一个实际上已经掉线的设备发指令。
 */
@Component
@ConditionalOnProperty(prefix = "xiaozhi.mqtt", name = "enabled", havingValue = "true")
public class MqttPresenceSweeper {

    private final DeviceChannelService deviceChannelService;
    private final MqttProperties properties;

    public MqttPresenceSweeper(DeviceChannelService deviceChannelService, MqttProperties properties) {
        this.deviceChannelService = deviceChannelService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${xiaozhi.mqtt.sweep-interval-ms:30000}")
    public void sweep() {
        deviceChannelService.sweepStale(Duration.ofSeconds(properties.getPresenceTtlSeconds()));
    }
}
