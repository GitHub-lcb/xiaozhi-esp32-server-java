package com.xiaozhi.stats.service;

import com.xiaozhi.common.model.bo.DialogueMetricBO;

import java.time.LocalDateTime;
import java.util.Map;

public interface DialogueMetricService {

    void record(DialogueMetricBO metric);

    /** 时间窗内的聚合：轮次、token 合计、平均时延 */
    Map<String, Object> summary(LocalDateTime from);
}
