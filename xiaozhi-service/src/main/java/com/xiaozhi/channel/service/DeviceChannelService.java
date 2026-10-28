package com.xiaozhi.channel.service;

import com.xiaozhi.common.model.bo.DeviceChannelBO;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * 设备接入通道的在线状态读写。
 * <p>
 * 通道表是「哪条通道最近有上行消息」的唯一事实来源：MQTT 桥收到消息就续期，
 * 巡检任务把超过 TTL 没消息的置为离线。主动推送、设备自检都读这里的结论。
 */
public interface DeviceChannelService {

    /** 通道建立：记录通道类型、客户端ID并置为在线 */
    void markOnline(String deviceId, String channel, String clientId);

    /** 通道上有上行消息：只续期，不改变通道类型 */
    void touch(String deviceId);

    /** 通道断开：置为离线 */
    void markOffline(String deviceId);

    /** 把超过 TTL 没有消息的通道置为离线，返回处理的条数 */
    int sweepStale(Duration ttl);

    DeviceChannelBO get(String deviceId);

    Map<String, DeviceChannelBO> mapByDeviceIds(Collection<String> deviceIds);
}
