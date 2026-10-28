package com.xiaozhi.stats.service.impl;

import com.xiaozhi.stats.dal.mysql.mapper.StatsMapper;
import com.xiaozhi.stats.service.StatsQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class StatsQueryServiceImpl implements StatsQueryService {

    @Resource
    private StatsMapper statsMapper;

    @Override
    public long countMessagesSince(LocalDateTime from) {
        return statsMapper.countMessagesSince(from);
    }

    @Override
    public long countActiveDevicesSince(LocalDateTime from) {
        return statsMapper.countActiveDevicesSince(from);
    }

    @Override
    public List<Map<String, Object>> dailyMessages(LocalDateTime from) {
        return statsMapper.dailyMessages(from);
    }

    @Override
    public List<Map<String, Object>> topDevices(LocalDateTime from, int limit) {
        return statsMapper.topDevices(from, limit);
    }
}
