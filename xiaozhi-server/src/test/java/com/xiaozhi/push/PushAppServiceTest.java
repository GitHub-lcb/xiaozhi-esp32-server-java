package com.xiaozhi.push;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.PushMessageBO;
import com.xiaozhi.common.model.req.PushMessageCreateReq;
import com.xiaozhi.common.model.resp.PushMessageResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.push.convert.PushMessageConvert;
import com.xiaozhi.push.service.PushMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主动推送的下发口径：只投在线设备、离线跳过并计数、按用户时展开名下设备、已发送的不许重发。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PushAppServiceTest {

    private static final String DEVICE_A = "aa:aa:aa:aa:aa:aa";
    private static final String DEVICE_B = "bb:bb:bb:bb:bb:bb";

    @Mock
    private PushMessageService pushMessageService;

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private RedisBroadcast redisBroadcast;

    @InjectMocks
    private PushAppService pushAppService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(pushAppService, "pushMessageConvert",
            Mappers.getMapper(PushMessageConvert.class));
    }

    @Test
    void sendOnlyDeliversToOnlineDevices() {
        when(pushMessageService.getBO(1L)).thenReturn(pending("device", DEVICE_A + "," + DEVICE_B));
        when(deviceChannelService.mapByDeviceIds(any()))
            .thenReturn(Map.of(DEVICE_A, channel(DEVICE_A, true), DEVICE_B, channel(DEVICE_B, false)));

        PushMessageResp resp = pushAppService.send(1L);

        verify(redisBroadcast).pushDevice(eq(DEVICE_A), any(), eq(1L));
        verify(redisBroadcast, never()).pushDevice(eq(DEVICE_B), any(), anyLong());
        verify(pushMessageService).markSent(1L, 1, 1, 2);
        assertThat(resp).isNotNull();
    }

    @Test
    void sendExpandsUserTargetsIntoTheirDevices() {
        when(pushMessageService.getBO(1L)).thenReturn(pending("user", "7"));
        DeviceBO device = new DeviceBO();
        device.setDeviceId(DEVICE_A);
        when(deviceService.listByUserId(7)).thenReturn(List.of(device));
        when(deviceChannelService.mapByDeviceIds(any()))
            .thenReturn(Map.of(DEVICE_A, channel(DEVICE_A, true)));

        pushAppService.send(1L);

        verify(redisBroadcast).pushDevice(eq(DEVICE_A), any(), eq(1L));
        verify(pushMessageService).markSent(1L, 1, 0, 1);
    }

    @Test
    void sendRejectsAlreadySentPush() {
        PushMessageBO sent = pending("device", DEVICE_A);
        sent.setState(PushMessageBO.STATE_SENT);
        when(pushMessageService.getBO(1L)).thenReturn(sent);

        assertThatThrownBy(() -> pushAppService.send(1L))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("只有待发送");
    }

    @Test
    void sendRejectsWhenNoTargetResolved() {
        when(pushMessageService.getBO(1L)).thenReturn(pending("device", " "));
        when(deviceChannelService.mapByDeviceIds(any())).thenReturn(Map.of());

        assertThatThrownBy(() -> pushAppService.send(1L))
            .isInstanceOf(OperationFailedException.class)
            .hasMessageContaining("没有解析到任何目标设备");
    }

    @Test
    void createKeepsTargetFields() {
        PushMessageCreateReq req = new PushMessageCreateReq();
        req.setTitle("提醒");
        req.setContent("该喝水了");
        req.setTargetType("device");
        req.setTargetValue(DEVICE_A);
        when(pushMessageService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PushMessageResp resp = pushAppService.create(req, 3);

        assertThat(resp.getTitle()).isEqualTo("提醒");
        assertThat(resp.getTargetValue()).isEqualTo(DEVICE_A);
    }

    private PushMessageBO pending(String targetType, String targetValue) {
        PushMessageBO bo = new PushMessageBO();
        bo.setPushId(1L);
        bo.setState(PushMessageBO.STATE_PENDING);
        bo.setTargetType(targetType);
        bo.setTargetValue(targetValue);
        bo.setTitle("提醒");
        bo.setContent("该喝水了");
        return bo;
    }

    private DeviceChannelBO channel(String deviceId, boolean online) {
        DeviceChannelBO bo = new DeviceChannelBO();
        bo.setDeviceId(deviceId);
        bo.setOnlineState(online ? DeviceChannelBO.ONLINE : DeviceChannelBO.OFFLINE);
        return bo;
    }
}
