package com.xiaozhi.reminder.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.ReminderBO;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderService {

    PageResult<ReminderBO> page(int pageNo, int pageSize, String deviceId, String state);

    ReminderBO getBO(Long reminderId);

    ReminderBO create(ReminderBO bo);

    ReminderBO cancel(Long reminderId);

    void delete(Long reminderId);

    /** 取出到点且仍待播报的提醒，按时间升序 */
    List<ReminderBO> listDue(LocalDateTime now, int limit);

    /** 播报成功：一次性置为已播报，重复任务顺延到下一次 */
    void markFired(ReminderBO reminder, LocalDateTime fireTime);

    /** 设备不在线跳过一次：一次性置为已错过，重复任务顺延到下一次 */
    void markMissed(ReminderBO reminder, LocalDateTime now);
}
