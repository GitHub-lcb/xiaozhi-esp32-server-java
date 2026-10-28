package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 一轮对话的用量与时延指标。 */
@Data
public class DialogueMetricBO {

    private Long metricId;
    private String deviceId;
    private Integer roleId;
    private String sessionId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private LocalDateTime createTime;
}
