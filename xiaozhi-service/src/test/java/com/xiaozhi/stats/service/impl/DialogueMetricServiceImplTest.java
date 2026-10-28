package com.xiaozhi.stats.service.impl;

import com.xiaozhi.common.model.bo.DialogueMetricBO;
import com.xiaozhi.stats.dal.mysql.dataobject.DialogueMetricDO;
import com.xiaozhi.stats.dal.mysql.mapper.DialogueMetricMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 指标口径：空窗口不能除以零，token 与时延按轮次求平均。 */
@ExtendWith(MockitoExtension.class)
class DialogueMetricServiceImplTest {

    @Mock
    private DialogueMetricMapper dialogueMetricMapper;

    @InjectMocks
    private DialogueMetricServiceImpl service;

    @Test
    void emptyWindowReturnsZeros() {
        when(dialogueMetricMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> summary = service.summary(LocalDateTime.now().minusDays(1));

        assertThat(summary.get("turns")).isEqualTo(0L);
        assertThat(summary.get("totalTokens")).isEqualTo(0L);
        assertThat(summary.get("avgLatencyMs")).isEqualTo(0L);
    }

    @Test
    void averagesLatencyAcrossTurns() {
        when(dialogueMetricMapper.selectList(any())).thenReturn(List.of(row(100, 200), row(200, 400)));

        Map<String, Object> summary = service.summary(LocalDateTime.now().minusDays(1));

        assertThat(summary.get("turns")).isEqualTo(2L);
        assertThat(summary.get("totalTokens")).isEqualTo(300L);
        assertThat(summary.get("avgLatencyMs")).isEqualTo(300L);
    }

    private DialogueMetricDO row(int tokens, long latency) {
        DialogueMetricDO row = new DialogueMetricDO();
        row.setTotalTokens(tokens);
        row.setLatencyMs(latency);
        return row;
    }
}
