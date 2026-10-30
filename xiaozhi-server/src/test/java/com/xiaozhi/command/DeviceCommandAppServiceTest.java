package com.xiaozhi.command;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.command.convert.DeviceCommandConvert;
import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import com.xiaozhi.common.model.req.DeviceCommandReq;
import com.xiaozhi.common.model.resp.DeviceCommandResp;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.communication.common.RedisBroadcast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 远程控制的翻译口径：动作要落到正确的设备工具与入参上，离线设备必须在下发前就拒绝，
 * 否则记录会一直停在「已下发」而没人知道到底执行没有。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceCommandAppServiceTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private DeviceCommandService deviceCommandService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private DeviceService deviceService;

    @Mock
    private RedisBroadcast redisBroadcast;

    @InjectMocks
    private DeviceCommandAppService appService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appService, "deviceCommandConvert",
            Mappers.getMapper(DeviceCommandConvert.class));
        ReflectionTestUtils.setField(appService, "rebootTool", "self.reboot");
        ReflectionTestUtils.setField(appService, "volumeTool", "self.audio_speaker.set_volume");
        ReflectionTestUtils.setField(appService, "volumeArgument", "volume");
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(onlineChannel());
        // 真实实现会把状态置为「已下发」，桩里同步这个语义，否则断言到的是 null
        when(deviceCommandService.record(any())).thenAnswer(invocation -> {
            DeviceCommandBO bo = invocation.getArgument(0);
            bo.setCommandId(9L);
            bo.setState(DeviceCommandBO.STATE_SENT);
            return bo;
        });
    }

    @Test
    void rebootMapsToDeviceRebootTool() {
        DeviceCommandResp resp = appService.send(DEVICE_ID, request("reboot", null, null, null), 1);

        ArgumentCaptor<DeviceCommandBO> captor = ArgumentCaptor.forClass(DeviceCommandBO.class);
        verify(deviceCommandService).record(captor.capture());
        assertThat(captor.getValue().getToolName()).isEqualTo("self.reboot");
        assertThat(captor.getValue().getArguments()).isEqualTo("{}");
        verify(redisBroadcast).sendDeviceCommand(eq(DEVICE_ID), eq(9L), eq("self.reboot"), eq("{}"));
        assertThat(resp.getState()).isEqualTo(DeviceCommandBO.STATE_SENT);
    }

    @Test
    void volumeCarriesTheRequestedLevel() {
        appService.send(DEVICE_ID, request("volume", 60, null, null), 1);

        ArgumentCaptor<DeviceCommandBO> captor = ArgumentCaptor.forClass(DeviceCommandBO.class);
        verify(deviceCommandService).record(captor.capture());
        assertThat(captor.getValue().getToolName()).isEqualTo("self.audio_speaker.set_volume");
        assertThat(captor.getValue().getArguments()).isEqualTo("{\"volume\":60}");
    }

    @Test
    void volumeWithoutLevelIsRejected() {
        assertThatThrownBy(() -> appService.send(DEVICE_ID, request("volume", null, null, null), 1))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("volume");
    }

    @Test
    void customActionRequiresToolName() {
        assertThatThrownBy(() -> appService.send(DEVICE_ID, request("custom", null, " ", null), 1))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("工具名");
    }

    @Test
    void customActionPassesArgumentsThrough() {
        appService.send(DEVICE_ID, request("custom", null, "self.display.set_theme", "{\"theme\":\"dark\"}"), 1);

        verify(redisBroadcast).sendDeviceCommand(eq(DEVICE_ID), eq(9L), eq("self.display.set_theme"),
            eq("{\"theme\":\"dark\"}"));
    }

    @Test
    void offlineDeviceIsRejectedBeforeRecording() {
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(null);

        assertThatThrownBy(() -> appService.send(DEVICE_ID, request("reboot", null, null, null), 1))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("设备不在线");
        verify(deviceCommandService, never()).record(any());
        verify(redisBroadcast, never()).sendDeviceCommand(any(), any(), any(), any());
    }

    private DeviceCommandReq request(String action, Integer volume, String toolName, String arguments) {
        DeviceCommandReq req = new DeviceCommandReq();
        req.setAction(action);
        req.setVolume(volume);
        req.setToolName(toolName);
        req.setArguments(arguments);
        return req;
    }

    private DeviceChannelBO onlineChannel() {
        DeviceChannelBO bo = new DeviceChannelBO();
        bo.setDeviceId(DEVICE_ID);
        bo.setOnlineState(DeviceChannelBO.ONLINE);
        return bo;
    }
}
