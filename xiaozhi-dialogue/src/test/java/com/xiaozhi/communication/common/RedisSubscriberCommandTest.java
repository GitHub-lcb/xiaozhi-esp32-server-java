package com.xiaozhi.communication.common;

import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.dialogue.llm.tool.mcp.device.DeviceMcpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 远程控制的回填契约：成功写成功、抛异常写失败，记录不能停在「已下发」。
 */
class RedisSubscriberCommandTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    private DeviceMcpService deviceMcpService;
    private DeviceCommandService deviceCommandService;
    private RedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        deviceMcpService = mock(DeviceMcpService.class);
        deviceCommandService = mock(DeviceCommandService.class);
        subscriber = new RedisSubscriber();
        ReflectionTestUtils.setField(subscriber, "deviceMcpService", deviceMcpService);
        ReflectionTestUtils.setField(subscriber, "deviceCommandService", deviceCommandService);
    }

    @Test
    void successfulCallIsRecordedAsSuccess() {
        when(deviceMcpService.callDeviceTool(eq(DEVICE_ID), eq("self.reboot"), any()))
            .thenReturn(Map.of("status", "ok"));

        subscriber.onDeviceCommand("{\"deviceId\":\"" + DEVICE_ID + "\",\"commandId\":7,"
            + "\"toolName\":\"self.reboot\",\"arguments\":\"{}\"}");

        verify(deviceCommandService).markResult(eq(7L), eq(true), anyString());
    }

    @Test
    void failedCallIsRecordedAsFailure() {
        when(deviceMcpService.callDeviceTool(anyString(), anyString(), any()))
            .thenThrow(new IllegalStateException("设备离线或未连接"));

        subscriber.onDeviceCommand("{\"deviceId\":\"" + DEVICE_ID + "\",\"commandId\":8,"
            + "\"toolName\":\"self.reboot\",\"arguments\":\"{}\"}");

        verify(deviceCommandService).markResult(eq(8L), eq(false), eq("设备离线或未连接"));
    }

    @Test
    void malformedMessageIsIgnored() {
        subscriber.onDeviceCommand("not-json");

        verify(deviceCommandService, never()).markResult(anyLong(), anyBoolean(), any());
    }
}
