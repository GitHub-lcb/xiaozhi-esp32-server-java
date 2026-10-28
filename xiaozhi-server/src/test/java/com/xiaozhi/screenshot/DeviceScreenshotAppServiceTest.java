package com.xiaozhi.screenshot;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import com.xiaozhi.common.model.bo.DeviceScreenshotBO;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.screenshot.convert.DeviceScreenshotConvert;
import com.xiaozhi.screenshot.service.DeviceScreenshotService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 截图下发：离线设备直接拒绝，在线设备要「落库 → 建截图记录 → 广播」三步齐全。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceScreenshotAppServiceTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private DeviceScreenshotService deviceScreenshotService;

    @Mock
    private DeviceCommandService deviceCommandService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private RedisBroadcast redisBroadcast;

    @InjectMocks
    private DeviceScreenshotAppService appService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appService, "deviceScreenshotConvert",
            Mappers.getMapper(DeviceScreenshotConvert.class));
        ReflectionTestUtils.setField(appService, "screenshotTool", "self.screen.snapshot");
        DeviceChannelBO channel = new DeviceChannelBO();
        channel.setDeviceId(DEVICE_ID);
        channel.setOnlineState(DeviceChannelBO.ONLINE);
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(channel);
        when(deviceCommandService.record(any())).thenAnswer(invocation -> {
            DeviceCommandBO bo = invocation.getArgument(0);
            bo.setCommandId(11L);
            return bo;
        });
        when(deviceScreenshotService.record(any())).thenAnswer(invocation -> {
            DeviceScreenshotBO bo = invocation.getArgument(0);
            bo.setShotId(3L);
            bo.setState(DeviceScreenshotBO.STATE_PENDING);
            return bo;
        });
    }

    @Test
    void captureRecordsCommandAndShotThenBroadcasts() {
        appService.capture(DEVICE_ID, 1);

        verify(deviceCommandService).record(any());
        verify(deviceScreenshotService).record(any());
        verify(redisBroadcast).sendDeviceCommand(eq(DEVICE_ID), eq(11L), eq("self.screen.snapshot"), eq("{}"));
    }

    @Test
    void offlineDeviceIsRejected() {
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(null);

        assertThatThrownBy(() -> appService.capture(DEVICE_ID, 1))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("设备不在线");
        verify(deviceScreenshotService, never()).record(any());
        verify(redisBroadcast, never()).sendDeviceCommand(any(), any(), any(), any());
    }
}
