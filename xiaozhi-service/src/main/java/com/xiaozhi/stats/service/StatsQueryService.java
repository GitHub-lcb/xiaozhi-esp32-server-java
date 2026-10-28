package com.xiaozhi.stats.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监控面板的只读聚合查询。Mapper 只允许出现在 service 模块，server 通过本接口取数。
 */
public interface StatsQueryService {

    long countMessagesSince(LocalDateTime from);

    long countActiveDevicesSince(LocalDateTime from);

    List<Map<String, Object>> dailyMessages(LocalDateTime from);

    List<Map<String, Object>> topDevices(LocalDateTime from, int limit);
}
