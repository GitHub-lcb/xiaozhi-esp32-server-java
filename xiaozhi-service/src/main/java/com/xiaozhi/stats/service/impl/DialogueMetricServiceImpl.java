package com.xiaozhi.stats.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.model.bo.DialogueMetricBO;
import com.xiaozhi.stats.dal.mysql.dataobject.DialogueMetricDO;
import com.xiaozhi.stats.dal.mysql.mapper.DialogueMetricMapper;
import com.xiaozhi.stats.service.DialogueMetricService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DialogueMetricServiceImpl implements DialogueMetricService {

    @Resource
    private DialogueMetricMapper dialogueMetricMapper;

    @Override
    public void record(DialogueMetricBO metric) {
        if (metric == null) {
            return;
        }
        try {
            DialogueMetricDO dataObject = new DialogueMetricDO();
            dataObject.setDeviceId(metric.getDeviceId());
            dataObject.setRoleId(metric.getRoleId());
            dataObject.setSessionId(metric.getSessionId());
            dataObject.setPromptTokens(metric.getPromptTokens());
            dataObject.setCompletionTokens(metric.getCompletionTokens());
            dataObject.setTotalTokens(metric.getTotalTokens());
            dataObject.setLatencyMs(metric.getLatencyMs());
            dialogueMetricMapper.insert(dataObject);
        } catch (RuntimeException e) {
            // 指标写失败不能影响对话
            log.warn("写入对话指标失败 deviceId={}", metric.getDeviceId(), e);
        }
    }

    @Override
    public Map<String, Object> summary(LocalDateTime from) {
        List<DialogueMetricDO> rows = dialogueMetricMapper.selectList(
            Wrappers.<DialogueMetricDO>lambdaQuery().ge(DialogueMetricDO::getCreateTime, from));
        long turns = rows.size();
        long totalTokens = rows.stream().mapToLong(row -> row.getTotalTokens() == null ? 0 : row.getTotalTokens()).sum();
        long latencySum = rows.stream().mapToLong(row -> row.getLatencyMs() == null ? 0 : row.getLatencyMs()).sum();
        Map<String, Object> result = new HashMap<>();
        result.put("turns", turns);
        result.put("totalTokens", totalTokens);
        result.put("avgLatencyMs", turns == 0 ? 0 : latencySum / turns);
        return result;
    }
}
