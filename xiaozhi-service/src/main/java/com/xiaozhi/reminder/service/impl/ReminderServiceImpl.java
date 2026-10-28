package com.xiaozhi.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.ReminderBO;
import com.xiaozhi.reminder.convert.ReminderConvert;
import com.xiaozhi.reminder.dal.mysql.dataobject.ReminderDO;
import com.xiaozhi.reminder.dal.mysql.mapper.ReminderMapper;
import com.xiaozhi.reminder.service.ReminderService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ReminderServiceImpl implements ReminderService {

    @Resource
    private ReminderMapper reminderMapper;

    @Resource
    private ReminderConvert reminderConvert;

    @Override
    public PageResult<ReminderBO> page(int pageNo, int pageSize, String deviceId, String state) {
        LambdaQueryWrapper<ReminderDO> query = Wrappers.<ReminderDO>lambdaQuery()
            .like(StringUtils.hasText(deviceId), ReminderDO::getDeviceId, deviceId)
            .eq(StringUtils.hasText(state), ReminderDO::getState, state)
            .orderByDesc(ReminderDO::getReminderId);
        IPage<ReminderDO> result = reminderMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<ReminderBO> list = result.getRecords().stream().map(reminderConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public ReminderBO getBO(Long reminderId) {
        return reminderConvert.toBO(require(reminderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderBO create(ReminderBO bo) {
        ReminderDO dataObject = reminderConvert.toDO(bo);
        dataObject.setState(ReminderBO.STATE_PENDING);
        if (!StringUtils.hasText(dataObject.getRepeatType())) {
            dataObject.setRepeatType(ReminderBO.REPEAT_ONCE);
        }
        reminderMapper.insert(dataObject);
        return reminderConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderBO cancel(Long reminderId) {
        ReminderDO dataObject = require(reminderId);
        if (ReminderBO.STATE_CANCELED.equals(dataObject.getState())) {
            return reminderConvert.toBO(dataObject);
        }
        if (!ReminderBO.STATE_PENDING.equals(dataObject.getState())) {
            throw new OperationFailedException("只有待播报的提醒可以取消");
        }
        dataObject.setState(ReminderBO.STATE_CANCELED);
        reminderMapper.updateById(dataObject);
        return reminderConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long reminderId) {
        require(reminderId);
        reminderMapper.deleteById(reminderId);
    }

    @Override
    public List<ReminderBO> listDue(LocalDateTime now, int limit) {
        return reminderMapper.selectList(Wrappers.<ReminderDO>lambdaQuery()
                .eq(ReminderDO::getState, ReminderBO.STATE_PENDING)
                .le(ReminderDO::getRemindTime, now)
                .orderByAsc(ReminderDO::getRemindTime)
                .last("limit " + Math.max(1, limit)))
            .stream()
            .map(reminderConvert::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFired(ReminderBO reminder, LocalDateTime fireTime) {
        ReminderDO dataObject = require(reminder.getReminderId());
        dataObject.setLastFireTime(fireTime);
        applyNextOrFinalState(dataObject, reminder.isRepeating(), fireTime, ReminderBO.STATE_FIRED);
        reminderMapper.updateById(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMissed(ReminderBO reminder, LocalDateTime now) {
        ReminderDO dataObject = require(reminder.getReminderId());
        applyNextOrFinalState(dataObject, reminder.isRepeating(), now, ReminderBO.STATE_MISSED);
        reminderMapper.updateById(dataObject);
        log.info("提醒跳过（设备不在线） reminderId={} deviceId={}", reminder.getReminderId(), reminder.getDeviceId());
    }

    /**
     * 重复任务：算出下一次播报时间并保持待播报；一次性任务：落到终态。
     * 服务器停机导致错过多次时，下一次时间要跳过已经过去的那几天，不能补播一串旧提醒。
     */
    private void applyNextOrFinalState(ReminderDO dataObject, boolean repeating, LocalDateTime now, String finalState) {
        if (!repeating) {
            dataObject.setState(finalState);
            return;
        }
        LocalDateTime next = nextOccurrence(dataObject.getRepeatType(), dataObject.getRemindTime(), now);
        dataObject.setRemindTime(next);
        dataObject.setState(ReminderBO.STATE_PENDING);
    }

    /**
     * 下一次播报时间：按天推进，跳过周末（工作日任务）与已经过去的时刻。
     */
    static LocalDateTime nextOccurrence(String repeatType, LocalDateTime current, LocalDateTime now) {
        if (current == null) {
            return null;
        }
        LocalDateTime next = current.plusDays(1);
        int guard = 0;
        while ((next.isBefore(now) || next.isEqual(now) || isSkippedWeekend(repeatType, next)) && guard++ < 3650) {
            next = next.plusDays(1);
        }
        return next;
    }

    private static boolean isSkippedWeekend(String repeatType, LocalDateTime time) {
        if (!ReminderBO.REPEAT_WEEKDAY.equals(repeatType)) {
            return false;
        }
        DayOfWeek dayOfWeek = time.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private ReminderDO require(Long reminderId) {
        ReminderDO dataObject = reminderId == null ? null : reminderMapper.selectById(reminderId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("提醒不存在");
        }
        return dataObject;
    }

    /** 供调度器记录当前时间，集中走 DateUtils 便于测试固定时钟 */
    LocalDateTime now() {
        return DateUtils.now();
    }
}
