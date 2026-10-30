package com.xiaozhi.stats;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stats.service.StatsQueryService;
import com.xiaozhi.utils.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * 监控面板的口径：离线判定要同时看通道状态与设备状态，趋势序列必须按天补齐，
 * 选天数要夹在 1-90 之间（别让前端传个 100000 天把查询拖死）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatsAppServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private StatsQueryService statsQueryService;

    @InjectMocks
    private StatsAppService statsAppService;

    @BeforeEach
    void setUp() {
        DateUtils.use(Clock.fixed(LocalDateTime.of(2026, 10, 28, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        DateUtils.reset();
    }

    @Test
    void overviewCountsOnlineByChannelOrDeviceState() {
        DeviceBO channelOnline = device("a");
        DeviceBO stateOnline = device("b");
        stateOnline.setState(DeviceBO.DEVICE_STATE_ONLINE);
        when(deviceService.listByUserId(1)).thenReturn(List.of(channelOnline, stateOnline, device("c")));
        DeviceChannelBO online = new DeviceChannelBO();
        online.setDeviceId("a");
        online.setOnlineState(DeviceChannelBO.ONLINE);
        when(deviceChannelService.mapByDeviceIds(any())).thenReturn(Map.of("a", online));
        when(statsQueryService.countMessagesSince(any())).thenReturn(12L, 40L);
        when(statsQueryService.countActiveDevicesSince(any())).thenReturn(2L);

        Map<String, Object> result = statsAppService.overview(1);

        assertThat(result.get("deviceTotal")).isEqualTo(3);
        assertThat(result.get("deviceOnline")).isEqualTo(2L);
        assertThat(result.get("messageToday")).isEqualTo(12L);
        assertThat(result.get("message7d")).isEqualTo(40L);
        assertThat(result.get("activeDevices7d")).isEqualTo(2L);
    }

    @Test
    void dailySeriesFillsMissingDaysWithZero() {
        when(statsQueryService.dailyMessages(any())).thenReturn(List.of(Map.of("day", "2026-10-28", "total", 5L)));

        List<Map<String, Object>> series = statsAppService.dailyMessages(3);

        assertThat(series).hasSize(3);
        assertThat(series.get(0).get("day")).isEqualTo("2026-10-26");
        assertThat(series.get(0).get("total")).isEqualTo(0L);
        assertThat(series.get(2).get("total")).isEqualTo(5L);
    }

    @Test
    void daysAreClampedToASafeRange() {
        when(statsQueryService.dailyMessages(any())).thenReturn(List.of());

        assertThat(statsAppService.dailyMessages(0)).hasSize(1);
        assertThat(statsAppService.dailyMessages(100000)).hasSize(90);
    }

    private DeviceBO device(String id) {
        DeviceBO bo = new DeviceBO();
        bo.setDeviceId(id);
        bo.setState(DeviceBO.DEVICE_STATE_OFFLINE);
        return bo;
    }
}
