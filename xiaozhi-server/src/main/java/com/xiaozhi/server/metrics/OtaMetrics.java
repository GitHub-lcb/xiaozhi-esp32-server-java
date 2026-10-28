package com.xiaozhi.server.metrics;

import com.xiaozhi.communication.registry.DialogueServerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * OTA 与实时通道的运维指标。
 * <p>
 * 只埋「排查时真的会看」的几个量：设备多久查一次升级、有多少次真的下发了新固件、
 * 当前有几个 dialogue 实例在线。指标名用点分（Micrometer 命名），Prometheus 导出时会变成下划线。
 */
@Component
public class OtaMetrics {

    private final Counter upgradeDelivered;
    private final Counter upgradeChecked;
    private final Counter upgradeSkipped;

    public OtaMetrics(MeterRegistry meterRegistry, DialogueServerRegistry dialogueServerRegistry) {
        this.upgradeDelivered = Counter.builder("xiaozhi.ota.upgrade.delivered")
            .description("OTA 下发固件升级的次数")
            .register(meterRegistry);
        this.upgradeChecked = Counter.builder("xiaozhi.ota.check")
            .description("设备 OTA 检查次数，按是否命中升级打标签")
            .tag("result", "upgrade")
            .register(meterRegistry);
        this.upgradeSkipped = Counter.builder("xiaozhi.ota.check")
            .description("设备 OTA 检查次数，按是否命中升级打标签")
            .tag("result", "no-upgrade")
            .register(meterRegistry);
        Gauge.builder("xiaozhi.dialogue.servers", dialogueServerRegistry,
                registry -> {
                    var servers = registry.getAvailableServers();
                    return servers == null ? 0 : servers.size();
                })
            .description("当前注册在线的 dialogue 实例数")
            .register(meterRegistry);
    }

    /** 每次 OTA 检查调用一次，按是否命中升级打标签 */
    public void recordCheck(boolean upgraded) {
        if (upgraded) {
            upgradeChecked.increment();
        } else {
            upgradeSkipped.increment();
        }
    }

    /** 真正把固件包下发出去时调用 */
    public void recordDelivered() {
        upgradeDelivered.increment();
    }
}
