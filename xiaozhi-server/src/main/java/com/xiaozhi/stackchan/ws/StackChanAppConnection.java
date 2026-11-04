package com.xiaozhi.stackchan.ws;

/**
 * App 侧的连接状态（对应 Go 版 {@code model.AppClient}）。
 * <p>
 * 一个 MAC 允许多条 App 连接（手机、平板、多个 App 实例同时看设备），因为订阅列表按连接对象
 * 增删，所以每个连接必须能独立识别：{@code appDeviceId} 取 App 握手时的 {@code deviceId}
 * 查询参数（当前 Flutter 版是 App 实例 UUID），用于同一实例重连时踢掉旧连接。
 */
public class StackChanAppConnection extends StackChanWsConnection {

    /** App 实例标识：握手 query 的 deviceId；缺省时退化为目标 MAC */
    private final String appDeviceId;

    /** App 登录用户 ID，用于排查与后续权限收敛 */
    private final Integer userId;

    public StackChanAppConnection(String mac, String appDeviceId, Integer userId) {
        super(mac);
        this.appDeviceId = appDeviceId;
        this.userId = userId;
    }

    public String getAppDeviceId() {
        return appDeviceId;
    }

    public Integer getUserId() {
        return userId;
    }
}
