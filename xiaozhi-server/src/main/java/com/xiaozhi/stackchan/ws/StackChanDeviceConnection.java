package com.xiaozhi.stackchan.ws;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * StackChan 设备侧的连接状态（对应 Go 版 {@code model.StackChanClient}）。
 * <p>
 * 一个 MAC 只有一条设备连接；重连时复用本对象的订阅/通话/投屏状态，因此这些状态只能在
 * 连接真正过期或清理时丢弃，不能在每次握手时重建。
 * <p>
 * 订阅列表用 {@link CopyOnWriteArrayList} 保存、以「对象身份」增删，遍历时拿快照，
 * 避免广播消息与 App 断线清理并发修改同一个列表。
 */
public class StackChanDeviceConnection extends StackChanWsConnection {

    /** 摄像头/JPEG 订阅者（App 连接） */
    private final CopyOnWriteArrayList<StackChanAppConnection> cameraSubscriptions = new CopyOnWriteArrayList<>();

    /** 音频/Opus 订阅者（App 连接） */
    private final CopyOnWriteArrayList<StackChanAppConnection> audioSubscriptions = new CopyOnWriteArrayList<>();

    /** 当前通话对端；同一时刻只允许一个 App 与设备通话 */
    private volatile StackChanAppConnection callAppConnection;

    /** 最近一次瞄准拍照请求的 App，设备回图时定向发给它 */
    private volatile StackChanAppConnection aimedTakePhotoAppConnection;

    /** 手机投屏模式：只有打开时服务端才会把 App 的 JPEG 帧转发给设备 */
    private volatile boolean phoneScreen;

    public StackChanDeviceConnection(String mac) {
        super(mac);
    }

    public StackChanAppConnection getCallAppConnection() {
        return callAppConnection;
    }

    public void setCallAppConnection(StackChanAppConnection connection) {
        this.callAppConnection = connection;
    }

    /** 抢占通话：空闲或已是同一 App 时返回 true（对应 Go 的「无通话或同一对端」判断） */
    public synchronized boolean trySetCallAppConnection(StackChanAppConnection app) {
        if (callAppConnection == null || callAppConnection == app) {
            callAppConnection = app;
            return true;
        }
        return false;
    }

    /** 仅当通话对端是给定 App 时才挂断，返回是否发生变更 */
    public synchronized boolean clearCallAppConnection(StackChanAppConnection app) {
        if (callAppConnection != null && callAppConnection == app) {
            callAppConnection = null;
            return true;
        }
        return false;
    }

    public StackChanAppConnection getAimedTakePhotoAppConnection() {
        return aimedTakePhotoAppConnection;
    }

    public void setAimedTakePhotoAppConnection(StackChanAppConnection connection) {
        this.aimedTakePhotoAppConnection = connection;
    }

    /** 仅当瞄准拍照请求方是给定 App 时才清除 */
    public void clearAimedTakePhotoAppConnection(StackChanAppConnection app) {
        if (aimedTakePhotoAppConnection != null && aimedTakePhotoAppConnection == app) {
            aimedTakePhotoAppConnection = null;
        }
    }

    public boolean isPhoneScreen() {
        return phoneScreen;
    }

    public void setPhoneScreen(boolean phoneScreen) {
        this.phoneScreen = phoneScreen;
    }

    public boolean addCameraSubscriber(StackChanAppConnection app) {
        return cameraSubscriptions.addIfAbsent(app);
    }

    public boolean removeCameraSubscriber(StackChanAppConnection app) {
        return cameraSubscriptions.remove(app);
    }

    public List<StackChanAppConnection> getCameraSubscribers() {
        return List.copyOf(cameraSubscriptions);
    }

    public int getCameraSubscriberCount() {
        return cameraSubscriptions.size();
    }

    public boolean addAudioSubscriber(StackChanAppConnection app) {
        return audioSubscriptions.addIfAbsent(app);
    }

    public boolean removeAudioSubscriber(StackChanAppConnection app) {
        return audioSubscriptions.remove(app);
    }

    public List<StackChanAppConnection> getAudioSubscribers() {
        return List.copyOf(audioSubscriptions);
    }

    public int getAudioSubscriberCount() {
        return audioSubscriptions.size();
    }
}
