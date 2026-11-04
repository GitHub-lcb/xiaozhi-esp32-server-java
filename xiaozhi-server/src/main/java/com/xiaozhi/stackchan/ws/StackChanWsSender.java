package com.xiaozhi.stackchan.ws;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 伴侣 WebSocket 的发送门面，屏蔽「发设备 / 发 App / 广播」与二进制帧/文本帧的编码差异。
 * <p>
 * 对应 Go 版的 {@code stackChanSendMessage / appSendMessage / SendStackChanMessage /
 * SendAppMessage}。所有发送都只做「尽力而为」：连接断开只记 debug 日志，不向业务抛异常，
 * 避免一条坏连接中断正在处理的转发流程。
 */
@Component
public class StackChanWsSender {

    /** 设备上线通知文案（与 Go 版逐字一致，端上按消息类型处理，文案仅用于展示/排查） */
    private static final String DEVICE_ONLINE_TEXT = "Your StackChan has been launched.";

    /** 设备离线通知文案 */
    private static final String DEVICE_OFFLINE_TEXT = "Your StackChan is offline.";

    @Resource
    private StackChanWsSessionRegistry sessionRegistry;

    // ==================== 发设备 ====================

    public void sendToDevice(StackChanDeviceConnection device, int msgType, byte[] payload) {
        if (device == null) {
            return;
        }
        device.sendBinary(StackChanWsCodec.encode(msgType, payload));
    }

    /** 原样透传已编码的帧（转发场景保持端上原始字节，不再二次编码） */
    public void sendRawToDevice(StackChanDeviceConnection device, byte[] frame) {
        if (device == null) {
            return;
        }
        device.sendBinary(frame);
    }

    public void sendTextToDevice(StackChanDeviceConnection device, String text) {
        if (device == null) {
            return;
        }
        device.sendText(text);
    }

    // ==================== 发 App ====================

    public void sendToApp(StackChanAppConnection app, int msgType, byte[] payload) {
        if (app == null) {
            return;
        }
        app.sendBinary(StackChanWsCodec.encode(msgType, payload));
    }

    public void sendRawToApp(StackChanAppConnection app, byte[] frame) {
        if (app == null) {
            return;
        }
        app.sendBinary(frame);
    }

    public void sendTextToApp(StackChanAppConnection app, String text) {
        if (app == null) {
            return;
        }
        app.sendText(text);
    }

    // ==================== 广播 ====================

    /** 向指定 MAC 下的所有 App 连接广播二进制消息 */
    public void broadcastToApps(String mac, int msgType, byte[] payload) {
        byte[] frame = StackChanWsCodec.encode(msgType, payload);
        broadcastRawToApps(mac, frame);
    }

    /** 向指定 MAC 下的所有 App 连接广播已编码帧 */
    public void broadcastRawToApps(String mac, byte[] frame) {
        List<StackChanAppConnection> apps = sessionRegistry.getApps(mac);
        for (StackChanAppConnection app : apps) {
            if (app.isOnline()) {
                app.sendBinary(frame);
            }
        }
    }

    /** 向指定 MAC 下的所有 App 连接广播文本消息（设备发来的文本帧原样转发） */
    public void broadcastTextToApps(String mac, String text) {
        List<StackChanAppConnection> apps = sessionRegistry.getApps(mac);
        for (StackChanAppConnection app : apps) {
            if (app.isOnline()) {
                app.sendText(text);
            }
        }
    }

    // ==================== 在线/离线通知 ====================

    /** 设备上线：通知该 MAC 下所有 App（对应 Go 版设备连接后进行广播） */
    public void notifyDeviceOnline(String mac) {
        broadcastToApps(mac, StackChanWsMessageType.DEVICE_ONLINE, text(DEVICE_ONLINE_TEXT));
    }

    /** 设备离线：通知该 MAC 下所有 App（对应 Go 版超时清理时的离线广播） */
    public void notifyDeviceOffline(String mac) {
        broadcastToApps(mac, StackChanWsMessageType.DEVICE_OFFLINE, text(DEVICE_OFFLINE_TEXT));
    }

    /** App 刚连上时告知其当前设备状态（Go 版只发给新连上的这个 App） */
    public void notifyAppDeviceOnline(StackChanAppConnection app) {
        sendToApp(app, StackChanWsMessageType.DEVICE_ONLINE, text(DEVICE_ONLINE_TEXT));
    }

    public void notifyAppDeviceOffline(StackChanAppConnection app) {
        sendToApp(app, StackChanWsMessageType.DEVICE_OFFLINE, text(DEVICE_OFFLINE_TEXT));
    }

    // ==================== 其它模块主动推送（与 Go 导出的两个方法对齐） ====================

    /** 按 MAC 给设备推消息；设备离线时静默丢弃 */
    public void sendStackChanMessage(String mac, int msgType, byte[] payload) {
        sendToDevice(sessionRegistry.getDevice(mac), msgType, payload);
    }

    /** 按 MAC 给该设备的所有 App 推消息；无连接时静默丢弃 */
    public void sendAppMessage(String mac, int msgType, byte[] payload) {
        broadcastToApps(mac, msgType, payload);
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
