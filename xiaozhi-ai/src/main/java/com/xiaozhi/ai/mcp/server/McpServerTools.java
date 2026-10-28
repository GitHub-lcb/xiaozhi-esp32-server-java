package com.xiaozhi.ai.mcp.server;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.device.service.DeviceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 对外暴露的 MCP 工具（SSE 接入点）。
 * <p>
 * 只读工具默认开启，写操作（发通知）默认关闭，由 xiaozhi.mcp.server.allow-write 控制；
 * MCP 客户端没有登录态，这里的可见范围是全库设备，属于管理员视角，
 * 部署到多租户环境前应先加按用户的鉴权。
 */
@Slf4j
@Component
public class McpServerTools {

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private com.xiaozhi.communication.common.RedisBroadcast redisBroadcast;

    @Value("${xiaozhi.mcp.server.allow-write:false}")
    private boolean allowWrite;

    @Tool(description = "列出系统中的设备，返回 设备ID|名称|设备状态|接入通道|通道是否在线")
    public String listDevices() {
        List<com.xiaozhi.common.model.bo.DeviceBO> devices = deviceService.listByStateAndType(null, null);
        List<String> lines = devices == null ? List.of() : devices.stream()
            .limit(50)
            .map(device -> {
                DeviceChannelBO channel = deviceChannelService.get(device.getDeviceId());
                return device.getDeviceId() + "|" + nullSafe(device.getDeviceName()) + "|" + nullSafe(device.getState())
                    + "|" + (channel == null ? "-" : channel.getChannel())
                    + "|" + (channel != null && channel.isOnline() ? "在线" : "离线");
            })
            .collect(Collectors.toList());
        return lines.isEmpty() ? "暂无设备" : String.join("\n", lines);
    }

    @Tool(description = "查询单台设备的状态，入参为设备ID（MAC 地址）")
    public String deviceStatus(String deviceId) {
        var device = deviceService.getBO(deviceId);
        if (device == null) {
            return "设备不存在：" + deviceId;
        }
        DeviceChannelBO channel = deviceChannelService.get(deviceId);
        return "设备=" + deviceId
            + "，状态=" + nullSafe(device.getState())
            + "，固件=" + nullSafe(device.getVersion())
            + "，通道=" + (channel == null ? "无" : channel.getChannel() + (channel.isOnline() ? "(在线)" : "(离线)") + "，最近消息=" + channel.getLastSeenTime());
    }

    @Tool(description = "让设备播报一条通知；需要服务端开启 xiaozhi.mcp.server.allow-write")
    public String sendNotification(String deviceId, String text) {
        if (!allowWrite) {
            return "写操作未开启：把 xiaozhi.mcp.server.allow-write 设为 true 后重启";
        }
        DeviceChannelBO channel = deviceChannelService.get(deviceId);
        if (channel == null || !channel.isOnline()) {
            return "设备离线，无法推送：" + deviceId;
        }
        redisBroadcast.pushDevice(deviceId, text, null);
        log.info("MCP 接入点下发通知 deviceId={}", deviceId);
        return "已下发";
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}