package com.xiaozhi.stackchan.ws;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 伴侣 WebSocket 连接池（对应 Go 版的 {@code stackChanClientPool / appClientPool}）。
 * <p>
 * <ul>
 *   <li>设备：按 MAC 单连接，「同 MAC 重连」复用同一个 {@link StackChanDeviceConnection}，
 *       以便保留通话与订阅状态；</li>
 *   <li>App：按 MAC 多连接，同一 MAC 下可能同时存在多台手机/多个 App 实例。</li>
 * </ul>
 * 所有读写都经 {@link ConcurrentHashMap} + 加锁的列表增删，遍历时拿快照，避免清理线程与
 * 业务线程并发修改抛 ConcurrentModificationException。
 */
@Component
public class StackChanWsSessionRegistry {

    private final ConcurrentHashMap<String, StackChanDeviceConnection> devicePool = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<StackChanAppConnection>> appPool = new ConcurrentHashMap<>();

    /** App 列表读改写用对象锁，避免 removeApp 删掉空列表与 addApp 并发写丢失连接 */
    private final Object appPoolLock = new Object();

    // ==================== 设备连接 ====================

    public StackChanDeviceConnection getDevice(String mac) {
        if (mac == null) {
            return null;
        }
        return devicePool.get(mac);
    }

    /** 取或建设备连接；重连时返回同一个对象，订阅/通话状态得以保留 */
    public StackChanDeviceConnection registerDevice(String mac) {
        return devicePool.computeIfAbsent(mac, StackChanDeviceConnection::new);
    }

    /** 仅当池中仍是该对象时才移除，避免「重连后旧连接的关闭回调」把新连接删掉 */
    public boolean removeDevice(String mac, StackChanDeviceConnection connection) {
        return mac != null && connection != null && devicePool.remove(mac, connection);
    }

    /**
     * 把被超时清理误移除的设备连接放回池（仅在清理与重连并发、移除后发现有新会话挂上时使用）。
     *
     * @return 是否放回成功；池中已有其它连接对象时不覆盖
     */
    public boolean restoreDevice(StackChanDeviceConnection connection) {
        if (connection == null || connection.getMac() == null) {
            return false;
        }
        return devicePool.putIfAbsent(connection.getMac(), connection) == null;
    }

    public List<StackChanDeviceConnection> allDevices() {
        return List.copyOf(devicePool.values());
    }

    public int deviceCount() {
        return devicePool.size();
    }

    // ==================== App 连接 ====================

    public void addApp(StackChanAppConnection app) {
        if (app == null) {
            return;
        }
        synchronized (appPoolLock) {
            appPool.computeIfAbsent(app.getMac(), key -> new ArrayList<>()).add(app);
        }
    }

    /** 从池中移除 App 连接，返回是否真的移除了（幂等，供正常关闭与超时清理共用） */
    public boolean removeApp(StackChanAppConnection app) {
        if (app == null) {
            return false;
        }
        synchronized (appPoolLock) {
            List<StackChanAppConnection> connections = appPool.get(app.getMac());
            if (connections == null) {
                return false;
            }
            boolean removed = connections.remove(app);
            if (removed && connections.isEmpty()) {
                appPool.remove(app.getMac(), connections);
            }
            return removed;
        }
    }

    /** 指定 MAC 下的 App 连接快照；无连接时返回空列表 */
    public List<StackChanAppConnection> getApps(String mac) {
        if (mac == null) {
            return List.of();
        }
        synchronized (appPoolLock) {
            List<StackChanAppConnection> connections = appPool.get(mac);
            return connections == null ? List.of() : List.copyOf(connections);
        }
    }

    /** 全量 App 连接快照，供心跳与超时清理遍历 */
    public List<StackChanAppConnection> allApps() {
        List<StackChanAppConnection> all = new ArrayList<>();
        synchronized (appPoolLock) {
            for (List<StackChanAppConnection> connections : appPool.values()) {
                all.addAll(connections);
            }
        }
        return all;
    }

    public int appCount() {
        synchronized (appPoolLock) {
            int count = 0;
            for (List<StackChanAppConnection> connections : appPool.values()) {
                count += connections.size();
            }
            return count;
        }
    }

    /** 按会话 ID 找 App 连接（afterConnectionClosed 里 session.getAttributes 已不可靠时使用） */
    public StackChanAppConnection findAppBySession(String mac, String sessionId) {
        if (mac == null || sessionId == null) {
            return null;
        }
        for (StackChanAppConnection app : getApps(mac)) {
            if (sessionId.equals(app.getSessionId())) {
                return app;
            }
        }
        return null;
    }

    /** 找同一 MAC 下同一 App 实例的在线连接（重连时用于踢掉旧连接） */
    public List<StackChanAppConnection> findAppsByAppDeviceId(String mac, String appDeviceId) {
        List<StackChanAppConnection> matched = new ArrayList<>();
        for (StackChanAppConnection app : getApps(mac)) {
            if (Objects.equals(app.getAppDeviceId(), appDeviceId)) {
                matched.add(app);
            }
        }
        return matched;
    }
}
