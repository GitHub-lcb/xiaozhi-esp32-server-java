package com.xiaozhi.command;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.command.convert.DeviceCommandConvert;
import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import com.xiaozhi.common.model.req.DeviceCommandPageReq;
import com.xiaozhi.common.model.req.DeviceCommandReq;
import com.xiaozhi.common.model.resp.DeviceCommandResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备远程控制：把管理端动作翻译成设备 MCP 工具调用。
 * <p>
 * 设备侧能力通过 MCP 暴露（音量、重启等），这里只做「动作 → 工具名 + 入参」的映射与审计记录，
 * 真正下发由持有会话的 dialogue 实例完成。
 */
@Slf4j
@Service
public class DeviceCommandAppService {

    /** 重启工具名，不同固件可能不一样，允许配置覆盖 */
    @Value("${xiaozhi.device-command.reboot-tool:self.reboot}")
    private String rebootTool;

    /** 音量工具名 */
    @Value("${xiaozhi.device-command.volume-tool:self.audio_speaker.set_volume}")
    private String volumeTool;

    @Value("${xiaozhi.device-command.volume-argument:volume}")
    private String volumeArgument;

    @Resource
    private DeviceCommandService deviceCommandService;

    @Resource
    private DeviceCommandConvert deviceCommandConvert;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private com.xiaozhi.device.service.DeviceService deviceService;

    @Resource
    private RedisBroadcast redisBroadcast;

    public PageResult<DeviceCommandResp> page(DeviceCommandPageReq req) {
        return deviceCommandService.page(req.getPageNo(), req.getPageSize(), req.getDeviceId(), req.getState())
            .map(deviceCommandConvert::toResp);
    }

    /**
     * 设备是否可下发指令。
     * <p>
     * 两条链路都要认：MQTT 设备在 sys_device_channel 里登记在线状态，
     * 而 WebSocket 设备根本不写这张表，只把状态同步到 sys_device.state。
     * 早先只查 channel，导致 WS 在线设备被一律判成离线。
     */
    private boolean isOnline(String deviceId) {
        DeviceChannelBO channel = deviceChannelService.get(deviceId);
        if (channel != null && channel.isOnline()) {
            return true;
        }
        var device = deviceService.getBO(deviceId);
        return device != null && "1".equals(device.getState());
    }

    /**
     * 下发一条控制指令：先判在线（离线直接拒绝，避免记录停在「已下发」），再落库并广播。
     */
    public DeviceCommandResp send(String deviceId, DeviceCommandReq req, Integer userId) {
        if (!isOnline(deviceId)) {
            throw new OperationFailedException("设备不在线，无法下发控制指令");
        }
        String toolName;
        String arguments;
        switch (req.getAction()) {
            case DeviceCommandBO.ACTION_REBOOT -> {
                toolName = rebootTool;
                arguments = "{}";
            }
            case DeviceCommandBO.ACTION_VOLUME -> {
                if (req.getVolume() == null) {
                    throw new OperationFailedException("设置音量时必须给出 volume");
                }
                toolName = volumeTool;
                Map<String, Object> args = new HashMap<>();
                args.put(volumeArgument, req.getVolume());
                arguments = JsonUtil.toJson(args);
            }
            default -> {
                if (!StringUtils.hasText(req.getToolName())) {
                    throw new OperationFailedException("自定义指令必须给出工具名");
                }
                toolName = req.getToolName().trim();
                arguments = StringUtils.hasText(req.getArguments()) ? req.getArguments() : "{}";
            }
        }

        DeviceCommandBO bo = new DeviceCommandBO();
        bo.setDeviceId(deviceId);
        bo.setAction(req.getAction());
        bo.setToolName(toolName);
        bo.setArguments(arguments);
        bo.setCreateUserId(userId);
        DeviceCommandBO recorded = deviceCommandService.record(bo);

        redisBroadcast.sendDeviceCommand(deviceId, recorded.getCommandId(), toolName, arguments);
        log.info("设备控制指令已下发 deviceId={} action={} tool={} commandId={}",
            deviceId, req.getAction(), toolName, recorded.getCommandId());
        return deviceCommandConvert.toResp(recorded);
    }
}
