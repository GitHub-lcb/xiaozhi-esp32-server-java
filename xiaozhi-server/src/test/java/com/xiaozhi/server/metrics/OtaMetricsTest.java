package com.xiaozhi.server.metrics;

import com.xiaozhi.communication.registry.DialogueServerInfo;
import com.xiaozhi.communication.registry.DialogueServerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 钉住指标口径：检查次数按「命中升级 / 未命中」分开计，下发次数只在真的下发时加一，
 * 在线实例数是 gauge（不该随抓取次数累加）。
 */
class OtaMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private DialogueServerRegistry dialogueServerRegistry;
    private OtaMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dialogueServerRegistry = mock(DialogueServerRegistry.class);
        when(dialogueServerRegistry.getAvailableServers()).thenReturn(List.of(new DialogueServerInfo()));
        metrics = new OtaMetrics(meterRegistry, dialogueServerRegistry);
    }

    @Test
    void checkWithoutUpgradeOnlyCountsTheNoUpgradeBucket() {
        metrics.recordCheck(false);

        assertThat(counter("xiaozhi.ota.check", "result", "no-upgrade")).isEqualTo(1.0);
        assertThat(counter("xiaozhi.ota.check", "result", "upgrade")).isZero();
        assertThat(counter("xiaozhi.ota.upgrade.delivered", null, null)).isZero();
    }

    @Test
    void matchedUpgradeCountsBothCheckAndDelivery() {
        metrics.recordCheck(true);
        metrics.recordDelivered();

        assertThat(counter("xiaozhi.ota.check", "result", "upgrade")).isEqualTo(1.0);
        assertThat(counter("xiaozhi.ota.upgrade.delivered", null, null)).isEqualTo(1.0);
    }

    @Test
    void dialogueServerGaugeReflectsRegistry() {
        when(dialogueServerRegistry.getAvailableServers())
            .thenReturn(List.of(new DialogueServerInfo(), new DialogueServerInfo()));

        Gauge gauge = meterRegistry.find("xiaozhi.dialogue.servers").gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(2.0);
    }

    private double counter(String name, String tagKey, String tagValue) {
        var search = meterRegistry.find(name);
        if (tagKey != null) {
            search = search.tag(tagKey, tagValue);
        }
        var counter = search.counter();
        return counter == null ? 0.0 : counter.count();
    }
}
