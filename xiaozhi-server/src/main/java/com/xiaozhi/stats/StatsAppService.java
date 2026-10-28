package com.xiaozhi.stats;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stats.service.StatsQueryService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控面板数据：设备活跃度、消息量趋势、Top 设备。
 * <p>
 * 全部按「当前登录用户自己的设备」统计，不做跨租户聚合。
 */
@Slf4j
@Service
public class StatsAppService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private StatsQueryService statsQueryService;

    public Map<String, Object> overview(Integer userId) {
        List<DeviceBO> devices = deviceService.listByUserId(userId);
        Map<String, DeviceChannelBO> channels = deviceChannelService.mapByDeviceIds(
            devices.stream().map(DeviceBO::getDeviceId).toList());

        long online = devices.stream()
            .filter(device -> {
                DeviceChannelBO channel = channels.get(device.getDeviceId());
                return (channel != null && channel.isOnline())
                    || DeviceBO.DEVICE_STATE_ONLINE.equals(device.getState());
            })
            .count();
        LocalDateTime todayStart = DateUtils.today().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(6);

        Map<String, Object> result = new HashMap<>();
        result.put("deviceTotal", devices.size());
        result.put("deviceOnline", online);
        result.put("messageToday", statsQueryService.countMessagesSince(todayStart));
        result.put("message7d", statsQueryService.countMessagesSince(weekStart));
        result.put("activeDevices7d", statsQueryService.countActiveDevicesSince(weekStart));
        return result;
    }

    /**
     * 最近 N 天的消息量，缺的日期补 0：折线/柱状图不能因为某天没消息就错位。
     */
    public List<Map<String, Object>> dailyMessages(int days) {
        int span = Math.min(Math.max(days, 1), 90);
        LocalDate start = DateUtils.today().minusDays(span - 1L);
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : statsQueryService.dailyMessages(start.atStartOfDay())) {
            Object day = row.get("day");
            Object total = row.get("total");
            if (day != null && total != null) {
                counts.put(day.toString(), Long.parseLong(total.toString()));
            }
        }
        List<Map<String, Object>> series = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            String day = start.plusDays(i).format(DAY);
            series.add(Map.of("day", day, "total", counts.getOrDefault(day, 0L)));
        }
        return series;
    }

    public List<Map<String, Object>> topDevices(int days, int limit) {
        int span = Math.min(Math.max(days, 1), 90);
        int size = Math.min(Math.max(limit, 1), 20);
        LocalDateTime from = DateUtils.today().minusDays(span - 1L).atStartOfDay();
        return statsQueryService.topDevices(from, size);
    }
}
