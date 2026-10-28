package com.xiaozhi.reminder.service.impl;

import com.xiaozhi.common.model.bo.ReminderBO;
import com.xiaozhi.reminder.convert.ReminderConvert;
import com.xiaozhi.reminder.dal.mysql.dataobject.ReminderDO;
import com.xiaozhi.reminder.dal.mysql.mapper.ReminderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提醒的重复规则：顺延要跳过已经过去的时刻和周末，否则会出现「补播一串昨天的闹钟」或者「周末早上 7 点响」。
 */
@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {

    @Mock
    private ReminderMapper reminderMapper;

    @InjectMocks
    private ReminderServiceImpl reminderService;

    ReminderServiceImplTest() {
        // 构造后由 Mockito 注入 mapper；convert 是普通字段，需要手动塞进真实的 MapStruct 实现
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderService, "reminderConvert", Mappers.getMapper(ReminderConvert.class));
    }

    @Test
    void dailyAdvancesToTomorrowWhenTodayAlreadyPassed() {
        LocalDateTime current = LocalDateTime.of(2026, 10, 28, 8, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);

        LocalDateTime next = ReminderServiceImpl.nextOccurrence(ReminderBO.REPEAT_DAILY, current, now);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 10, 29, 8, 0));
    }

    @Test
    void dailySkipsDaysTheServerWasDown() {
        LocalDateTime current = LocalDateTime.of(2026, 10, 20, 8, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);

        LocalDateTime next = ReminderServiceImpl.nextOccurrence(ReminderBO.REPEAT_DAILY, current, now);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 10, 29, 8, 0));
    }

    @Test
    void weekdayJumpsOverTheWeekend() {
        // 2026-10-30 是周五，下一次要跳到周一
        LocalDateTime current = LocalDateTime.of(2026, 10, 30, 7, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 8, 0);

        LocalDateTime next = ReminderServiceImpl.nextOccurrence(ReminderBO.REPEAT_WEEKDAY, current, now);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 11, 2, 7, 0));
    }

    @Test
    void markFiredKeepsRepeatingReminderPending() {
        ReminderDO reminder = pending(ReminderBO.REPEAT_DAILY, LocalDateTime.of(2026, 10, 28, 8, 0));
        when(reminderMapper.selectById(1L)).thenReturn(reminder);
        ReminderBO bo = boOf(reminder);

        reminderService.markFired(bo, LocalDateTime.of(2026, 10, 29, 8, 0));

        ArgumentCaptor<ReminderDO> captor = ArgumentCaptor.forClass(ReminderDO.class);
        verify(reminderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(ReminderBO.STATE_PENDING);
        assertThat(captor.getValue().getRemindTime()).isEqualTo(LocalDateTime.of(2026, 10, 30, 8, 0));
        assertThat(captor.getValue().getLastFireTime()).isEqualTo(LocalDateTime.of(2026, 10, 29, 8, 0));
    }

    @Test
    void markFiredFinishesOnceReminder() {
        ReminderDO reminder = pending(ReminderBO.REPEAT_ONCE, LocalDateTime.of(2026, 10, 28, 8, 0));
        when(reminderMapper.selectById(1L)).thenReturn(reminder);

        reminderService.markFired(boOf(reminder), LocalDateTime.of(2026, 10, 28, 8, 0));

        ArgumentCaptor<ReminderDO> captor = ArgumentCaptor.forClass(ReminderDO.class);
        verify(reminderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(ReminderBO.STATE_FIRED);
    }

    @Test
    void markMissedReschedulesRepeatingReminder() {
        ReminderDO reminder = pending(ReminderBO.REPEAT_DAILY, LocalDateTime.of(2026, 10, 28, 8, 0));
        when(reminderMapper.selectById(1L)).thenReturn(reminder);

        reminderService.markMissed(boOf(reminder), LocalDateTime.of(2026, 10, 28, 9, 0));

        ArgumentCaptor<ReminderDO> captor = ArgumentCaptor.forClass(ReminderDO.class);
        verify(reminderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(ReminderBO.STATE_PENDING);
        assertThat(captor.getValue().getRemindTime()).isEqualTo(LocalDateTime.of(2026, 10, 29, 8, 0));
    }

    @Test
    void markMissedMarksOnceReminderAsMissed() {
        ReminderDO reminder = pending(ReminderBO.REPEAT_ONCE, LocalDateTime.of(2026, 10, 28, 8, 0));
        when(reminderMapper.selectById(1L)).thenReturn(reminder);

        reminderService.markMissed(boOf(reminder), LocalDateTime.of(2026, 10, 28, 9, 0));

        ArgumentCaptor<ReminderDO> captor = ArgumentCaptor.forClass(ReminderDO.class);
        verify(reminderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(ReminderBO.STATE_MISSED);
    }

    private ReminderDO pending(String repeatType, LocalDateTime remindTime) {
        ReminderDO reminder = new ReminderDO();
        reminder.setReminderId(1L);
        reminder.setDeviceId("aa:bb:cc:dd:ee:ff");
        reminder.setContent("该喝水了");
        reminder.setRepeatType(repeatType);
        reminder.setRemindTime(remindTime);
        reminder.setState(ReminderBO.STATE_PENDING);
        return reminder;
    }

    private ReminderBO boOf(ReminderDO dataObject) {
        ReminderBO bo = new ReminderBO();
        bo.setReminderId(dataObject.getReminderId());
        bo.setDeviceId(dataObject.getDeviceId());
        bo.setRepeatType(dataObject.getRepeatType());
        bo.setRemindTime(dataObject.getRemindTime());
        bo.setState(dataObject.getState());
        return bo;
    }
}
