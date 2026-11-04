package com.xiaozhi.stackchan.ws;

/**
 * StackChan 伴侣 WebSocket 二进制协议的消息类型。
 * <p>
 * 帧格式：{@code 1 字节 msgType + 4 字节大端长度 + payload}。这些数值同时被
 * StackChan 固件（hal_ws_avatar.cpp 的 DataType）与 Flutter App（msg_type.dart）使用，
 * 与 Go 版后端 {@code server/internal/web_socket/web_socket.go} 的常量逐项一致，禁止改值。
 */
public final class StackChanWsMessageType {

    /** Opus 音频帧（设备 → App 转发；App → 设备时 payload 前 12 字节为目标 MAC） */
    public static final int OPUS = 0x01;
    /** JPEG 视频帧（设备 → 相机订阅者；App → 设备时受 phoneScreen 开关限制） */
    public static final int JPEG = 0x02;
    /** 表情控制（App → 设备） */
    public static final int CONTROL_AVATAR = 0x03;
    /** 舵机动作控制（App → 设备） */
    public static final int CONTROL_MOTION = 0x04;
    /** 摄像头订阅开启（App → 设备，设备回 App 时表示开始推流） */
    public static final int ON_CAMERA = 0x05;
    /** 摄像头订阅关闭 */
    public static final int OFF_CAMERA = 0x06;
    /** 文本消息（透传，payload 是 JSON 文本） */
    public static final int TEXT_MESSAGE = 0x07;
    /** App 请求与设备通话 */
    public static final int REQUEST_CALL = 0x09;
    /** 设备拒绝通话 */
    public static final int REFUSE_CALL = 0x0A;
    /** 设备同意通话 */
    public static final int AGREE_CALL = 0x0B;
    /** 挂断通话 */
    public static final int HANGUP_CALL = 0x0C;
    /** 更新设备名（App → 服务端/设备） */
    public static final int UPDATE_DEVICE_NAME = 0x0D;
    /** 查询设备名（设备或 App 向服务端查询） */
    public static final int GET_DEVICE_NAME = 0x0E;
    /** 对端正在通话中（服务端 → App） */
    public static final int IN_CALL = 0x0F;
    /** 服务端心跳 ping（二进制帧，非 WebSocket 控制帧） */
    public static final int PING = 0x10;
    /** 心跳 pong */
    public static final int PONG = 0x11;
    /** 手机投屏开启 */
    public static final int ON_PHONE_SCREEN = 0x12;
    /** 手机投屏关闭 */
    public static final int OFF_PHONE_SCREEN = 0x13;
    /** 跳舞序列 */
    public static final int DANCE = 0x14;
    /** 查询当前姿态 */
    public static final int GET_AVATAR_POSTURE = 0x15;
    /** 设备离线（服务端 → App） */
    public static final int DEVICE_OFFLINE = 0x16;
    /** 设备在线（服务端 → App） */
    public static final int DEVICE_ONLINE = 0x17;
    /** 音频订阅开启 */
    public static final int ON_AUDIO = 0x18;
    /** 音频订阅关闭 */
    public static final int OFF_AUDIO = 0x19;
    /** 瞄准拍照（App 请求，设备回图） */
    public static final int AIMED_TAKE_PHOTO = 0x1A;

    private StackChanWsMessageType() {
    }
}
