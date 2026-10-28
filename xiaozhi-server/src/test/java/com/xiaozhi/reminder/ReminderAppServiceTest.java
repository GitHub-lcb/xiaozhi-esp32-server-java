package com.xiaozhi.reminder;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.ReminderBO;
import com.xiaozhi.common.model.resp.ReminderResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.reminder.convert.ReminderConvert;
import com.xiaozhi.reminder.service.ReminderService;
import com.xiaozhi.utils.DateUtils;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 到点扫描的下发口径：在线播报并记账，离线跳过一次，不能把同一条提醒重复投给设备。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderAppServiceTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private ReminderService reminderService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @Mock
    private RedisBroadcast redisBroadcast;

    @InjectMocks
    private ReminderAppService reminderAppService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderAppService, "reminderConvert", Mappers.getMapper(ReminderConvert.class));
        DateUtils.use(Clock.fixed(LocalDateTime.of(2026, 10, 28, 8, 0)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        DateUtils.reset();
    }

    @Test
    void dueReminderOnOnlineDeviceIsSpoken() {
        ReminderBO reminder = reminder();
        when(reminderService.listDue(any(), any(Integer.class))).thenReturn(List.of(reminder));
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(channel(true));

        reminderAppService.scanDueReminders();

        verify(redisBroadcast).pushDevice(eq(DEVICE_ID), eq("该喝水了"), eq(null));
        verify(reminderService).markFired(eq(reminder), any());
        verify(reminderService, never()).markMissed(any(), any());
    }

    @Test
    void dueReminderOnOfflineDeviceIsMarkedMissed() {
        ReminderBO reminder = reminder();
        when(reminderService.listDue(any(), any(Integer.class))).thenReturn(List.of(reminder));
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(channel(false));

        reminderAppService.scanDueReminders();

        verify(redisBroadcast, never()).pushDevice(any(), any(), anyLong());
        verify(reminderService).markMissed(eq(reminder), any());
        verify(reminderService, never()).markFired(any(), any());
    }

    @Test
    void nothingHappensWhenNoReminderIsDue() {
        when(reminderService.listDue(any(), any(Integer.class))).thenReturn(List.of());

        reminderAppService.scanDueReminders();

        verify(redisBroadcast, never()).pushDevice(any(), any(), anyLong());
    }

    private ReminderBO reminder() {
        ReminderBO reminder = new ReminderBO();
        reminder.setReminderId(1L);
        reminder.setDeviceId(DEVICE_ID);
        reminder.setContent("该喝水了");
        reminder.setRepeatType(ReminderBO.REPEAT_ONCE);
        reminder.setRemindTime(LocalDateTime.of(2026, 10, 28, 8, 0));
        reminder.setState(ReminderBO.STATE_PENDING);
        return reminder;
    }

    private DeviceChannelBO channel(boolean online) {
        DeviceChannelBO bo = new DeviceChannelBO();
        bo.setDeviceId(DEVICE_ID);
        bo.setOnlineState(online ? DeviceChannelBO.ONLINE : DeviceChannelBO.OFFLINE);
        return bo;
    }
}
