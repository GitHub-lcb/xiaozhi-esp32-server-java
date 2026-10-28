package com.xiaozhi.ai.mcp.server;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.device.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接入点的两条红线：写操作默认关闭、离线设备不下发。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class McpServerToolsTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private RedisBroadcast redisBroadcast;

    @InjectMocks
    private McpServerTools tools;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tools, "allowWrite", false);
        DeviceBO device = new DeviceBO();
        device.setDeviceId(DEVICE_ID);
        device.setDeviceName("客厅音箱");
        device.setState(DeviceBO.DEVICE_STATE_ONLINE);
        when(deviceService.listByStateAndType(any(), any())).thenReturn(List.of(device));
        when(deviceService.getBO(DEVICE_ID)).thenReturn(device);
        DeviceChannelBO channel = new DeviceChannelBO();
        channel.setDeviceId(DEVICE_ID);
        channel.setChannel(DeviceChannelBO.CHANNEL_MQTT);
        channel.setOnlineState(DeviceChannelBO.ONLINE);
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(channel);
    }

    @Test
    void listDevicesRendersRowPerDevice() {
        String result = tools.listDevices();

        assertThat(result).contains(DEVICE_ID).contains("客厅音箱").contains("mqtt").contains("在线");
    }

    @Test
    void deviceStatusReportsUnknownDevice() {
        when(deviceService.getBO("unknown")).thenReturn(null);

        assertThat(tools.deviceStatus("unknown")).contains("设备不存在");
    }

    @Test
    void writeToolIsDisabledByDefault() {
        String result = tools.sendNotification(DEVICE_ID, "该喝水了");

        assertThat(result).contains("写操作未开启");
        verify(redisBroadcast, never()).pushDevice(any(), any(), any());
    }

    @Test
    void offlineDeviceIsNotPushable() {
        ReflectionTestUtils.setField(tools, "allowWrite", true);
        DeviceChannelBO offline = new DeviceChannelBO();
        offline.setDeviceId(DEVICE_ID);
        offline.setOnlineState(DeviceChannelBO.OFFLINE);
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(offline);

        assertThat(tools.sendNotification(DEVICE_ID, "该喝水了")).contains("离线");
        verify(redisBroadcast, never()).pushDevice(any(), any(), any());
    }

    @Test
    void onlineDeviceGetsPushWhenWriteEnabled() {
        ReflectionTestUtils.setField(tools, "allowWrite", true);

        assertThat(tools.sendNotification(DEVICE_ID, "该喝水了")).isEqualTo("已下发");
        verify(redisBroadcast).pushDevice(eq(DEVICE_ID), eq("该喝水了"), eq(null));
    }
}