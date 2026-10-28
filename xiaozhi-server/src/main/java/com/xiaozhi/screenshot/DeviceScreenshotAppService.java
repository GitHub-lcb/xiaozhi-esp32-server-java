package com.xiaozhi.screenshot;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import com.xiaozhi.common.model.bo.DeviceScreenshotBO;
import com.xiaozhi.common.model.req.DeviceScreenshotPageReq;
import com.xiaozhi.common.model.resp.DeviceScreenshotResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.screenshot.convert.DeviceScreenshotConvert;
import com.xiaozhi.screenshot.service.DeviceScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 设备截图：复用远程控制通道，调用设备截图工具并等待它回传图片地址。
 */
@Slf4j
@Service
public class DeviceScreenshotAppService {

    /** 设备截图工具名，不同固件可能不一样 */
    @Value("${xiaozhi.device-command.screenshot-tool:self.screen.snapshot}")
    private String screenshotTool;

    @Resource
    private DeviceScreenshotService deviceScreenshotService;

    @Resource
    private DeviceScreenshotConvert deviceScreenshotConvert;

    @Resource
    private DeviceCommandService deviceCommandService;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private RedisBroadcast redisBroadcast;

    public PageResult<DeviceScreenshotResp> page(DeviceScreenshotPageReq req) {
        return deviceScreenshotService.page(req.getPageNo(), req.getPageSize(), req.getDeviceId(), req.getState())
            .map(deviceScreenshotConvert::toResp);
    }

    public DeviceScreenshotResp capture(String deviceId, Integer userId) {
        DeviceChannelBO channel = deviceChannelService.get(deviceId);
        if (channel == null || !channel.isOnline()) {
            throw new OperationFailedException("设备不在线，无法截图");
        }
        DeviceCommandBO command = new DeviceCommandBO();
        command.setDeviceId(deviceId);
        command.setAction(DeviceCommandBO.ACTION_CUSTOM);
        command.setToolName(screenshotTool);
        command.setArguments("{}");
        command.setCreateUserId(userId);
        DeviceCommandBO recordedCommand = deviceCommandService.record(command);

        DeviceScreenshotBO shot = new DeviceScreenshotBO();
        shot.setDeviceId(deviceId);
        shot.setCommandId(recordedCommand.getCommandId());
        shot.setCreateUserId(userId);
        DeviceScreenshotBO recordedShot = deviceScreenshotService.record(shot);

        redisBroadcast.sendDeviceCommand(deviceId, recordedCommand.getCommandId(), screenshotTool, "{}");
        log.info("设备截图指令已下发 deviceId={} shotId={} tool={}", deviceId, recordedShot.getShotId(), screenshotTool);
        return deviceScreenshotConvert.toResp(recordedShot);
    }
}
