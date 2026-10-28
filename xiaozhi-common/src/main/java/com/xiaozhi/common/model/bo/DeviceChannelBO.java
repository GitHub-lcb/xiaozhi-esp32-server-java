package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备接入通道与在线状态。
 * <p>
 * WebSocket 与 MQTT 两条通道共用这一份记录：哪个通道最近有上行消息，channel 就是哪个。
 * MQTT 侧靠周期心跳续期，超过 TTL 没消息由巡检任务置为离线。
 */
@Data
public class DeviceChannelBO {

    public static final String CHANNEL_WEBSOCKET = "websocket";
    public static final String CHANNEL_MQTT = "mqtt";

    public static final String ONLINE = "1";
    public static final String OFFLINE = "0";

    private String deviceId;
    private String channel;
    private String clientId;
    private String onlineState;
    private LocalDateTime lastSeenTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isOnline() {
        return ONLINE.equals(onlineState);
    }
}
