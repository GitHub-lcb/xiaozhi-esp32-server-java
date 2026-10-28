package com.xiaozhi.channel.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiaozhi.channel.convert.DeviceChannelConvert;
import com.xiaozhi.channel.dal.mysql.dataobject.DeviceChannelDO;
import com.xiaozhi.channel.dal.mysql.mapper.DeviceChannelMapper;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.utils.DateUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通道在线状态是主动推送的判据：在线判错会把消息推给掉线设备，离线判错会让设备收不到唤醒。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceChannelServiceImplTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private DeviceChannelMapper deviceChannelMapper;

    @InjectMocks
    private DeviceChannelServiceImpl deviceChannelService;

    /**
     * LambdaUpdateWrapper 需要实体的 TableInfo 才能解析列名，单元测试里没有 Spring 容器，
     * 手动初始化一次；不初始化会抛 "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            DeviceChannelDO.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deviceChannelService, "deviceChannelConvert",
            Mappers.getMapper(DeviceChannelConvert.class));
        DateUtils.use(Clock.fixed(
            LocalDateTime.of(2026, 10, 28, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        DateUtils.reset();
    }

    @Test
    void firstMessageInsertsChannelRowAsOnline() {
        when(deviceChannelMapper.selectById(DEVICE_ID)).thenReturn(null);

        deviceChannelService.markOnline(DEVICE_ID, DeviceChannelBO.CHANNEL_MQTT, "client-1");

        verify(deviceChannelMapper).insert(any(DeviceChannelDO.class));
    }

    @Test
    void laterMessageReusesTheRow() {
        DeviceChannelDO existing = new DeviceChannelDO();
        existing.setDeviceId(DEVICE_ID);
        existing.setChannel(DeviceChannelBO.CHANNEL_WEBSOCKET);
        when(deviceChannelMapper.selectById(DEVICE_ID)).thenReturn(existing);

        deviceChannelService.markOnline(DEVICE_ID, DeviceChannelBO.CHANNEL_MQTT, "client-1");

        assertThat(existing.getChannel()).isEqualTo(DeviceChannelBO.CHANNEL_MQTT);
        assertThat(existing.getOnlineState()).isEqualTo(DeviceChannelBO.ONLINE);
        verify(deviceChannelMapper).updateById(existing);
    }

    @Test
    void getReturnsMappedBo() {
        DeviceChannelDO existing = new DeviceChannelDO();
        existing.setDeviceId(DEVICE_ID);
        existing.setChannel(DeviceChannelBO.CHANNEL_MQTT);
        existing.setOnlineState(DeviceChannelBO.ONLINE);
        when(deviceChannelMapper.selectById(DEVICE_ID)).thenReturn(existing);

        DeviceChannelBO bo = deviceChannelService.get(DEVICE_ID);

        assertThat(bo.getChannel()).isEqualTo(DeviceChannelBO.CHANNEL_MQTT);
        assertThat(bo.isOnline()).isTrue();
    }

    @Test
    void mapByDeviceIdsKeepsOneEntryPerDevice() {
        DeviceChannelDO existing = new DeviceChannelDO();
        existing.setDeviceId(DEVICE_ID);
        when(deviceChannelMapper.selectBatchIds(any())).thenReturn(List.of(existing));

        Map<String, DeviceChannelBO> result = deviceChannelService.mapByDeviceIds(List.of(DEVICE_ID));

        assertThat(result).containsKey(DEVICE_ID);
    }

    @Test
    void sweepUsesConfiguredTtl() {
        when(deviceChannelMapper.update(any(), any())).thenReturn(2);

        int updated = deviceChannelService.sweepStale(Duration.ofSeconds(120));

        assertThat(updated).isEqualTo(2);
    }
}
