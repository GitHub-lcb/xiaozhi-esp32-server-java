package com.xiaozhi.stackchan.ws;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.device.DeviceAppService;
import com.xiaozhi.device.service.DeviceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * App → StackChan 方向的消息处理（逐条对应 Go 版 {@code readAppClientMessage}）。
 * <p>
 * 关键差异：App 发往设备的业务消息，payload 前 12 字节是纯 MAC（12 hex，无分隔符），
 * 服务端必须先 {@link StackChanWsCodec#splitMacPayload(byte[])} 归一化成小写冒号 MAC 再查连接池；
 * 转发给设备时要把这 12 字节前缀去掉（设备只认业务数据），而 Go 版在部分消息上是整帧透传，
 * 这里按「设备能正确解析」的语义统一处理，注释里逐条标明与 Go 的对应关系。
 * <p>
 * 订阅类消息（On/OffCamera、On/OffAudio、手机投屏）的 payload 本身就是 MAC，直接整段归一化。
 */
@Slf4j
@Component
public class StackChanWsAppHandler {

    @Resource
    private StackChanWsSessionRegistry sessionRegistry;

    @Resource
    private StackChanWsSender sender;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceAppService deviceAppService;

    /** App 发来的文本帧：原样转发给对应设备 */
    public void handleText(StackChanAppConnection app, String text) {
        StackChanDeviceConnection device = sessionRegistry.getDevice(app.getMac());
        if (device != null) {
            sender.sendTextToDevice(device, text);
        }
    }

    public void handle(StackChanAppConnection app, byte[] frame, StackChanWsCodec.ParsedFrame message) {
        switch (message.msgType()) {
            case StackChanWsMessageType.PONG -> {
                // 心跳回应，lastActive 已在收发入口刷新
            }
            case StackChanWsMessageType.GET_DEVICE_NAME -> handleGetDeviceName(app);
            case StackChanWsMessageType.UPDATE_DEVICE_NAME -> handleUpdateDeviceName(app, frame, message.payload());
            case StackChanWsMessageType.OPUS -> handleOpus(app, message.payload());
            case StackChanWsMessageType.JPEG -> handleJpeg(app, message.payload());
            case StackChanWsMessageType.CONTROL_AVATAR,
                 StackChanWsMessageType.CONTROL_MOTION -> handleControl(app, message.payload(), message.msgType());
            case StackChanWsMessageType.TEXT_MESSAGE -> handleTextMessage(app, message.payload());
            case StackChanWsMessageType.REQUEST_CALL -> handleRequestCall(app, message.payload());
            case StackChanWsMessageType.HANGUP_CALL -> handleHangupCall(app, frame);
            case StackChanWsMessageType.ON_AUDIO -> handleOnAudio(app, frame, message.payload());
            case StackChanWsMessageType.OFF_AUDIO -> handleOffAudio(app, frame, message.payload());
            case StackChanWsMessageType.ON_CAMERA -> handleOnCamera(app, frame, message.payload());
            case StackChanWsMessageType.OFF_CAMERA -> handleOffCamera(app, frame, message.payload());
            case StackChanWsMessageType.ON_PHONE_SCREEN -> handleOnPhoneScreen(app, frame, message.payload());
            case StackChanWsMessageType.OFF_PHONE_SCREEN -> handleOffPhoneScreen(app, frame, message.payload());
            case StackChanWsMessageType.DANCE -> forwardToOwnDevice(app, frame);
            case StackChanWsMessageType.GET_AVATAR_POSTURE -> forwardToOwnDevice(app, frame);
            case StackChanWsMessageType.AIMED_TAKE_PHOTO -> handleAimedTakePhoto(app, frame);
            default -> {
                log.info("收到未识别的 App 二进制消息类型: {}, mac={}", message.msgType(), app.getMac());
                forwardToOwnDevice(app, frame);
            }
        }
    }

    /** 查询设备名：直接读设备档案的 deviceName 回给 App（对应 Go 的 service.GetDeviceName） */
    private void handleGetDeviceName(StackChanAppConnection app) {
        DeviceBO device;
        try {
            device = deviceService.getBO(app.getMac());
        } catch (RuntimeException e) {
            log.warn("查询设备名称失败, mac={}", app.getMac(), e);
            return;
        }
        if (device == null || !StringUtils.hasText(device.getDeviceName())) {
            log.info("查询到的设备名称为空, mac={}", app.getMac());
            return;
        }
        sender.sendToApp(app, StackChanWsMessageType.GET_DEVICE_NAME,
                device.getDeviceName().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 更新设备名：Go 版只做整帧转发（设备把 payload 直接当名字保存），Java 版在转发之外
     * 还要落库（任务要求用 {@link DeviceAppService#update(String, DeviceUpdateReq)}，只改 name）。
     * payload 兼容两种端上格式：12 字节 MAC 前缀 + 名字，或纯名字（老 App）。
     */
    private void handleUpdateDeviceName(StackChanAppConnection app, byte[] frame, byte[] payload) {
        // 先按 Go 版语义把原始帧转发给设备与其它 App（低延迟，不阻塞在落库上），再尽力落库
        StackChanDeviceConnection device = sessionRegistry.getDevice(app.getMac());
        if (device != null) {
            sender.sendRawToDevice(device, frame);
        }
        sender.broadcastRawToApps(app.getMac(), frame);
        persistDeviceName(app.getMac(), payload);
    }

    private void persistDeviceName(String mac, byte[] payload) {
        if (payload == null || payload.length == 0) {
            return;
        }
        String name;
        StackChanWsCodec.MacPayload macPayload = StackChanWsCodec.splitMacPayload(payload);
        if (macPayload != null && macPayload.mac().equals(mac)) {
            name = new String(macPayload.data(), StandardCharsets.UTF_8).trim();
        } else {
            name = new String(payload, StandardCharsets.UTF_8).trim();
        }
        if (!StringUtils.hasText(name)) {
            return;
        }
        try {
            DeviceUpdateReq updateReq = new DeviceUpdateReq();
            // DeviceUpdateReq 还有 roleId/location，这里只改名字，其余字段保持 null 不落库
            updateReq.setDeviceName(name);
            deviceAppService.update(mac, updateReq);
            log.info("设备名称已更新, mac={}", mac);
        } catch (RuntimeException e) {
            log.warn("更新设备名称失败, mac={}", mac, e);
        }
    }

    /** Opus：按 payload 前缀 MAC 找到目标设备，去掉前缀后转发 */
    private void handleOpus(StackChanAppConnection app, byte[] payload) {
        StackChanWsCodec.MacPayload target = splitTargetMac(payload, "Opus", app);
        if (target == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(target.mac());
        if (device != null) {
            sender.sendToDevice(device, StackChanWsMessageType.OPUS, target.data());
        }
    }

    /** JPEG：只有目标设备打开手机投屏时才转发，避免 App 的镜像帧打断设备本地显示 */
    private void handleJpeg(StackChanAppConnection app, byte[] payload) {
        StackChanWsCodec.MacPayload target = splitTargetMac(payload, "Jpeg", app);
        if (target == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(target.mac());
        if (device != null && device.isPhoneScreen()) {
            sender.sendToDevice(device, StackChanWsMessageType.JPEG, target.data());
        }
    }

    /** 表情/舵机控制：按 payload 前缀 MAC 转发，设备离线只记日志 */
    private void handleControl(StackChanAppConnection app, byte[] payload, int msgType) {
        StackChanWsCodec.MacPayload target = splitTargetMac(payload, "Control", app);
        if (target == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(target.mac());
        if (device == null) {
            log.info("StackChan 当前离线, mac={}", target.mac());
            return;
        }
        sender.sendToDevice(device, msgType, target.data());
    }

    /** 文本消息（JSON）：设备与其它 App 都收到去掉 MAC 前缀后的业务数据 */
    private void handleTextMessage(StackChanAppConnection app, byte[] payload) {
        StackChanWsCodec.MacPayload target = splitTargetMac(payload, "Text", app);
        if (target == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(target.mac());
        if (device != null) {
            sender.sendToDevice(device, StackChanWsMessageType.TEXT_MESSAGE, target.data());
        }
        sender.broadcastToApps(target.mac(), StackChanWsMessageType.TEXT_MESSAGE, target.data());
    }

    /**
     * 请求通话：同一设备同一时刻只允许一个 App 通话。
     * 目标设备空闲（或本来就是该 App）才转发请求，否则回 0x0F 提示对端忙。
     */
    private void handleRequestCall(StackChanAppConnection app, byte[] payload) {
        StackChanWsCodec.MacPayload target = splitTargetMac(payload, "RequestCall", app);
        if (target == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(target.mac());
        if (device == null) {
            log.info("请求通话的目标 StackChan 不在线, mac={}", target.mac());
            return;
        }
        if (device.trySetCallAppConnection(app)) {
            sender.sendToDevice(device, StackChanWsMessageType.REQUEST_CALL, target.data());
        } else {
            sender.sendToApp(app, StackChanWsMessageType.IN_CALL,
                    "The other party is currently in a call".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 挂断通话：Go 版全局扫描设备池找出「通话对端是这个 App」的设备。
     * 挂断后同步清理摄像头/音频订阅，列表变空时通知设备停流。
     */
    private void handleHangupCall(StackChanAppConnection app, byte[] frame) {
        for (StackChanDeviceConnection device : sessionRegistry.allDevices()) {
            if (device.getCallAppConnection() != app) {
                continue;
            }
            device.setCallAppConnection(null);
            sender.sendRawToDevice(device, frame);

            boolean cameraRemoved = device.removeCameraSubscriber(app);
            if (cameraRemoved && device.getCameraSubscriberCount() == 0) {
                sender.sendToDevice(device, StackChanWsMessageType.OFF_CAMERA, null);
            }
            boolean audioRemoved = device.removeAudioSubscriber(app);
            if (audioRemoved && device.getAudioSubscriberCount() == 0) {
                sender.sendToDevice(device, StackChanWsMessageType.OFF_AUDIO, null);
            }
            // 与 Go 一致：只处理第一个匹配设备
            return;
        }
    }

    /** 开启音频订阅：首次加入时把原始帧转发给设备，让设备开始推 Opus */
    private void handleOnAudio(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OnAudio", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device != null && device.addAudioSubscriber(app)) {
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 关闭音频订阅：仅当这个 App 真的在列表里且列表清空时通知设备停流 */
    private void handleOffAudio(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OffAudio", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device == null) {
            return;
        }
        boolean removed = device.removeAudioSubscriber(app);
        if (removed && device.getAudioSubscriberCount() == 0) {
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 开启摄像头订阅：首次加入时把原始帧转发给设备，让设备开始推 JPEG */
    private void handleOnCamera(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OnCamera", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device != null && device.addCameraSubscriber(app)) {
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 关闭摄像头订阅：仅当这个 App 真的在列表里且列表清空时通知设备停流 */
    private void handleOffCamera(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OffCamera", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device == null) {
            return;
        }
        boolean removed = device.removeCameraSubscriber(app);
        if (removed && device.getCameraSubscriberCount() == 0) {
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 打开手机投屏：状态从 false 变 true 时才需要转发给设备 */
    private void handleOnPhoneScreen(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OnPhoneScreen", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device != null && !device.isPhoneScreen()) {
            device.setPhoneScreen(true);
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 关闭手机投屏：状态从 true 变 false 时才需要转发给设备 */
    private void handleOffPhoneScreen(StackChanAppConnection app, byte[] frame, byte[] payload) {
        String mac = requirePayloadMac(payload, "OffPhoneScreen", app);
        if (mac == null) {
            return;
        }
        StackChanDeviceConnection device = sessionRegistry.getDevice(mac);
        if (device != null && device.isPhoneScreen()) {
            device.setPhoneScreen(false);
            sender.sendRawToDevice(device, frame);
        }
    }

    /** 瞄准拍照：记录发起者，设备回图时定向转发给它，然后把请求原样转给设备 */
    private void handleAimedTakePhoto(StackChanAppConnection app, byte[] frame) {
        StackChanDeviceConnection device = sessionRegistry.getDevice(app.getMac());
        if (device == null) {
            return;
        }
        device.setAimedTakePhotoAppConnection(app);
        sender.sendRawToDevice(device, frame);
    }

    /** 按 App 所属设备转发（Dance / GetAvatarPosture 等 payload 不带目标 MAC 的消息） */
    private void forwardToOwnDevice(StackChanAppConnection app, byte[] frame) {
        StackChanDeviceConnection device = sessionRegistry.getDevice(app.getMac());
        if (device != null) {
            sender.sendRawToDevice(device, frame);
        }
    }

    private StackChanWsCodec.MacPayload splitTargetMac(byte[] payload, String messageName,
                                                       StackChanAppConnection app) {
        StackChanWsCodec.MacPayload target = StackChanWsCodec.splitMacPayload(payload);
        if (target == null) {
            log.warn("{} 消息 payload 缺少目标 MAC 前缀，已丢弃, app mac={}", messageName, app.getMac());
        }
        return target;
    }

    private String requirePayloadMac(byte[] payload, String messageName, StackChanAppConnection app) {
        String mac = StackChanWsCodec.payloadMac(payload);
        if (mac == null) {
            log.warn("{} 消息 payload 不是合法 MAC，已丢弃, app mac={}", messageName, app.getMac());
        }
        return mac;
    }
}
