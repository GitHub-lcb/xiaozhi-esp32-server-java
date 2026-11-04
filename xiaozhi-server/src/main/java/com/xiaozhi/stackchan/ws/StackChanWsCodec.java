package com.xiaozhi.stackchan.ws;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * StackChan 伴侣 WebSocket 的二进制帧编解码与 MAC 前缀处理。
 * <p>
 * 帧格式与 Go 版 {@code createMessage / parseBinaryMessage} 一致：
 * 1 字节 msgType + 4 字节大端 payload 长度 + payload。
 * <p>
 * App 发往设备的消息，payload 前 12 字节是纯 MAC（无分隔符的 12 个 hex 字符，见改造后的
 * Flutter App {@code deviceMac.toUint8List()}）；设备发往 App 的消息不带这个前缀。
 * 服务端连接池的 key 统一是小写冒号 MAC，因此取前缀后必须先 normalize 再查池。
 */
public final class StackChanWsCodec {

    /** 协议固定头长度：1 字节类型 + 4 字节长度 */
    private static final int HEADER_LENGTH = 5;

    /** App → 服务端 payload 中目标 MAC 前缀长度（纯 12 hex） */
    public static final int MAC_PREFIX_LENGTH = 12;

    /** 兼容少量端上仍发冒号分隔 MAC 的情况：17 字节前缀 */
    private static final int SEPARATED_MAC_PREFIX_LENGTH = 17;

    private static final Pattern PURE_MAC_PATTERN = Pattern.compile("^[0-9a-f]{12}$");

    private static final Pattern SEPARATED_MAC_PATTERN = Pattern.compile("^([0-9a-f]{2}[:-]){5}[0-9a-f]{2}$");

    private StackChanWsCodec() {
    }

    /** 解析后的帧：msgType 与 payload（payload 一定是新数组，可安全持有） */
    public record ParsedFrame(int msgType, byte[] payload) {
    }

    /** 从 App 消息 payload 里拆出的目标 MAC 与剩余业务数据 */
    public record MacPayload(String mac, byte[] data) {
    }

    /**
     * 解析二进制帧；长度不足或头部声明长度与实际不一致时返回 {@code null}
     * （与 Go 版 parseBinaryMessage 的「不转发非法帧」行为一致）。
     */
    public static ParsedFrame parse(byte[] frame) {
        if (frame == null || frame.length < HEADER_LENGTH) {
            return null;
        }
        int msgType = frame[0] & 0xFF;
        int dataLength = ((frame[1] & 0xFF) << 24)
                | ((frame[2] & 0xFF) << 16)
                | ((frame[3] & 0xFF) << 8)
                | (frame[4] & 0xFF);
        if (dataLength < 0 || frame.length - HEADER_LENGTH != dataLength) {
            return null;
        }
        byte[] payload = Arrays.copyOfRange(frame, HEADER_LENGTH, frame.length);
        return new ParsedFrame(msgType, payload);
    }

    /** 按协议封装一帧；payload 为 {@code null} 时按空 payload 处理 */
    public static byte[] encode(int msgType, byte[] payload) {
        int dataLength = payload == null ? 0 : payload.length;
        byte[] frame = new byte[HEADER_LENGTH + dataLength];
        frame[0] = (byte) msgType;
        frame[1] = (byte) ((dataLength >>> 24) & 0xFF);
        frame[2] = (byte) ((dataLength >>> 16) & 0xFF);
        frame[3] = (byte) ((dataLength >>> 8) & 0xFF);
        frame[4] = (byte) (dataLength & 0xFF);
        if (dataLength > 0) {
            System.arraycopy(payload, 0, frame, HEADER_LENGTH, dataLength);
        }
        return frame;
    }

    /** 按协议封装一个文本 payload（UTF-8） */
    public static byte[] encodeString(int msgType, String text) {
        return encode(msgType, text == null ? null : text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 拆分带目标 MAC 前缀的 App 消息 payload（Opus/JPEG/表情/动作/文本/通话请求等）。
     * <p>
     * 优先识别 12 字节纯 MAC；若首部是 17 字节冒号 MAC 也接受（老端兼容）。
     * 无法解析出合法 MAC 时返回 {@code null}，调用方按 Go 版行为记日志后丢弃。
     */
    public static MacPayload splitMacPayload(byte[] payload) {
        if (payload == null) {
            return null;
        }
        String mac = null;
        int prefixLength = 0;
        if (payload.length >= SEPARATED_MAC_PREFIX_LENGTH && isSeparatedMacPrefix(payload)) {
            mac = normalizeMac(new String(payload, 0, SEPARATED_MAC_PREFIX_LENGTH, StandardCharsets.US_ASCII));
            prefixLength = SEPARATED_MAC_PREFIX_LENGTH;
        } else if (payload.length >= MAC_PREFIX_LENGTH) {
            mac = normalizeMac(new String(payload, 0, MAC_PREFIX_LENGTH, StandardCharsets.US_ASCII));
            prefixLength = MAC_PREFIX_LENGTH;
        }
        if (mac == null) {
            return null;
        }
        return new MacPayload(mac, Arrays.copyOfRange(payload, prefixLength, payload.length));
    }

    /**
     * 解析「整个 payload 就是 MAC」的消息（OnCamera/OffCamera/OnAudio/OffAudio/OnPhoneScreen/
     * OffPhoneScreen 等）。容忍尾部补零的定长实现。
     */
    public static String payloadMac(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        int end = payload.length;
        while (end > 0 && payload[end - 1] == 0) {
            end--;
        }
        if (end == 0) {
            return null;
        }
        return normalizeMac(new String(payload, 0, end, StandardCharsets.US_ASCII));
    }

    /**
     * 归一化 MAC：接受大小写与 {@code :}/{@code -} 分隔符，也接受 12 位纯 hex，
     * 统一输出小写冒号格式（服务端设备主键口径）；不合法返回 {@code null}。
     */
    public static String normalizeMac(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', ':');
        if (PURE_MAC_PATTERN.matcher(value).matches()) {
            StringBuilder mac = new StringBuilder(SEPARATED_MAC_PREFIX_LENGTH);
            for (int i = 0; i < value.length(); i += 2) {
                if (i > 0) {
                    mac.append(':');
                }
                mac.append(value, i, i + 2);
            }
            return mac.toString();
        }
        if (SEPARATED_MAC_PATTERN.matcher(value).matches()) {
            return value;
        }
        return null;
    }

    private static boolean isSeparatedMacPrefix(byte[] payload) {
        return payload[2] == ':' && payload[5] == ':' && payload[8] == ':' && payload[11] == ':' && payload[14] == ':';
    }
}
