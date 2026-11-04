package com.xiaozhi.stackchan.ws;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.device.service.DeviceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * StackChan 设备 → App 方向的消息处理（逐条对应 Go 版 {@code readStackChanMessage}）。
 * <p>
 * 设备发来的二进制帧不带 MAC 前缀，转发给 App 时一律保持原始字节；只有当某个订阅者
 * 列表为空（或全部已断开）时，服务端才反向给设备回一条 OffCamera / OffAudio 让它停止推流。
 * 文本帧原样广播给该设备的所有 App。
 */
@Slf4j
@Component
public class StackChanWsDeviceHandler {

    @Resource
    private StackChanWsSender sender;

    @Resource
    private DeviceService deviceService;

    /** 设备发来的文本帧：原样广播给该 MAC 下的所有 App */
    public void handleText(StackChanDeviceConnection device, String text) {
        sender.broadcastTextToApps(device.getMac(), text);
    }

    public void handle(StackChanDeviceConnection device, byte[] frame, StackChanWsCodec.ParsedFrame message) {
        switch (message.msgType()) {
            // 心跳回应与「设备侧不应主动发」的控制类消息，服务端不做动作
            case StackChanWsMessageType.PONG,
                 StackChanWsMessageType.CONTROL_AVATAR,
                 StackChanWsMessageType.CONTROL_MOTION,
                 StackChanWsMessageType.ON_CAMERA,
                 StackChanWsMessageType.OFF_CAMERA -> {
                // no-op，与 Go 版相同
            }
            case StackChanWsMessageType.REFUSE_CALL -> handleRefuseCall(device, frame);
            case StackChanWsMessageType.AGREE_CALL -> handleAgreeCall(device, frame);
            case StackChanWsMessageType.HANGUP_CALL -> handleHangupCall(device, frame);
            case StackChanWsMessageType.GET_DEVICE_NAME -> handleGetDeviceName(device);
            case StackChanWsMessageType.OPUS -> forwardAudioFrame(device, frame);
            case StackChanWsMessageType.JPEG -> forwardCameraFrame(device, frame);
            case StackChanWsMessageType.GET_AVATAR_POSTURE -> sender.broadcastRawToApps(device.getMac(), frame);
            case StackChanWsMessageType.AIMED_TAKE_PHOTO -> forwardAimedTakePhoto(device, frame);
            default -> {
                log.info("收到未识别的设备二进制消息类型: {}, mac={}", message.msgType(), device.getMac());
                sender.broadcastRawToApps(device.getMac(), frame);
            }
        }
    }

    /** 设备拒绝通话：转给对方 App 后清空通话对端 */
    private void handleRefuseCall(StackChanDeviceConnection device, byte[] frame) {
        StackChanAppConnection app = device.getCallAppConnection();
        if (app == null) {
            return;
        }
        sender.sendRawToApp(app, frame);
        device.setCallAppConnection(null);
    }

    /**
     * 设备同意通话：先把 App 加入订阅列表再通知（避免第一帧 Opus 抢在订阅之前被丢掉），
     * 订阅列表由空变非空时补发 OnCamera / OnAudio 让设备开始推流。
     */
    private void handleAgreeCall(StackChanDeviceConnection device, byte[] frame) {
        StackChanAppConnection app = device.getCallAppConnection();
        if (app == null) {
            return;
        }
        boolean cameraAdded = device.addCameraSubscriber(app);
        if (cameraAdded && device.getCameraSubscriberCount() == 1) {
            sender.sendToDevice(device, StackChanWsMessageType.ON_CAMERA, null);
        }
        boolean audioAdded = device.addAudioSubscriber(app);
        if (audioAdded && device.getAudioSubscriberCount() == 1) {
            sender.sendToDevice(device, StackChanWsMessageType.ON_AUDIO, null);
        }
        sender.sendRawToApp(app, frame);
    }

    /** 设备挂断通话：把挂断帧转给 App，并把它从摄像头/音频订阅里摘掉，列表空时通知设备停流 */
    private void handleHangupCall(StackChanDeviceConnection device, byte[] frame) {
        StackChanAppConnection app = device.getCallAppConnection();
        if (app == null) {
            return;
        }
        sender.sendRawToApp(app, frame);
        device.clearCallAppConnection(app);

        boolean cameraRemoved = device.removeCameraSubscriber(app);
        if (cameraRemoved && device.getCameraSubscriberCount() == 0) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_CAMERA, null);
        }
        boolean audioRemoved = device.removeAudioSubscriber(app);
        if (audioRemoved && device.getAudioSubscriberCount() == 0) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_AUDIO, null);
        }
    }

    /** 设备查询自己的名字：从设备档案取 deviceName 回给设备（对应 Go 的 service.GetDeviceName） */
    private void handleGetDeviceName(StackChanDeviceConnection device) {
        String name = loadDeviceName(device.getMac());
        if (!StringUtils.hasText(name)) {
            log.info("查询到的设备名称为空, mac={}", device.getMac());
            return;
        }
        sender.sendToDevice(device, StackChanWsMessageType.GET_DEVICE_NAME,
                name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Opus 音频帧：转发给所有音频订阅者；没有一个在线订阅者时回 OffAudio 让设备停止采集。
     * 与 Go 版一致，这里不主动清理「已断线但还没被超时清理」的订阅者。
     */
    private void forwardAudioFrame(StackChanDeviceConnection device, byte[] frame) {
        List<StackChanAppConnection> subscribers = device.getAudioSubscribers();
        if (subscribers.isEmpty()) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_AUDIO, null);
            return;
        }
        boolean sent = false;
        for (StackChanAppConnection subscriber : subscribers) {
            if (subscriber.isOnline()) {
                sender.sendRawToApp(subscriber, frame);
                sent = true;
            }
        }
        if (!sent) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_AUDIO, null);
        }
    }

    /** JPEG 视频帧：转发给所有摄像头订阅者；没有一个在线订阅者时回 OffCamera */
    private void forwardCameraFrame(StackChanDeviceConnection device, byte[] frame) {
        List<StackChanAppConnection> subscribers = device.getCameraSubscribers();
        if (subscribers.isEmpty()) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_CAMERA, null);
            return;
        }
        boolean sent = false;
        for (StackChanAppConnection subscriber : subscribers) {
            if (subscriber.isOnline()) {
                sender.sendRawToApp(subscriber, frame);
                sent = true;
            }
        }
        if (!sent) {
            sender.sendToDevice(device, StackChanWsMessageType.OFF_CAMERA, null);
        }
    }

    /** 瞄准拍照回图：只发给发起 AimedTakePhoto 的那个 App */
    private void forwardAimedTakePhoto(StackChanDeviceConnection device, byte[] frame) {
        StackChanAppConnection app = device.getAimedTakePhotoAppConnection();
        if (app != null) {
            sender.sendRawToApp(app, frame);
        }
    }

    private String loadDeviceName(String mac) {
        try {
            DeviceBO device = deviceService.getBO(mac);
            return device == null ? null : device.getDeviceName();
        } catch (RuntimeException e) {
            log.warn("查询设备名称失败, mac={}", mac, e);
            return null;
        }
    }
}
