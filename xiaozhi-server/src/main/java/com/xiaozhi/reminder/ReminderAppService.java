package com.xiaozhi.reminder;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.ReminderBO;
import com.xiaozhi.common.model.req.ReminderCreateReq;
import com.xiaozhi.common.model.req.ReminderPageReq;
import com.xiaozhi.common.model.resp.ReminderResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.reminder.convert.ReminderConvert;
import com.xiaozhi.reminder.service.ReminderScanExecutor;
import com.xiaozhi.reminder.service.ReminderService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音提醒：到点把播报文本投给设备，复用主动推送的 Redis 通道。
 * <p>
 * 定时调度与多实例互斥在 service 模块的 {@code ReminderScanTask}（server 层不直连 Redis），
 * 本类实现 {@link ReminderScanExecutor} 承接业务与事务；真正开口的仍是持有会话的 dialogue 实例。
 */
@Slf4j
@Service
public class ReminderAppService implements ReminderScanExecutor {

    /** 单次扫描最多处理条数，避免启动后一次性补播大量历史提醒 */
    private static final int BATCH_SIZE = 50;

    @Resource
    private ReminderService reminderService;

    @Resource
    private ReminderConvert reminderConvert;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private RedisBroadcast redisBroadcast;

    public PageResult<ReminderResp> page(ReminderPageReq req) {
        return reminderService.page(req.getPageNo(), req.getPageSize(), req.getDeviceId(), req.getState())
            .map(reminderConvert::toResp);
    }

    public ReminderResp create(ReminderCreateReq req, Integer userId) {
        ReminderBO bo = new ReminderBO();
        bo.setDeviceId(req.getDeviceId());
        bo.setTitle(req.getTitle());
        bo.setContent(req.getContent());
        bo.setRemindTime(req.getRemindTime());
        bo.setRepeatType(req.getRepeatType());
        bo.setCreateUserId(userId);
        return reminderConvert.toResp(reminderService.create(bo));
    }

    public ReminderResp cancel(Long reminderId) {
        return reminderConvert.toResp(reminderService.cancel(reminderId));
    }

    public void delete(Long reminderId) {
        reminderService.delete(reminderId);
    }

    /**
     * 到点扫描：在线设备直接播报，离线跳过一次（重复任务顺延，一次性任务记为错过）。
     * <p>
     * 事务边界在 server（本方法），定时与多实例互斥在 service 的 {@code ReminderScanTask}：
     * 那边先抢 Redisson 锁，再调用本实现，锁覆盖到事务提交之后才释放。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanDueReminders() {
        LocalDateTime now = DateUtils.now();
        List<ReminderBO> due = reminderService.listDue(now, BATCH_SIZE);
        if (due.isEmpty()) {
            return;
        }
        for (ReminderBO reminder : due) {
            DeviceChannelBO channel = deviceChannelService.get(reminder.getDeviceId());
            boolean online = channel != null && channel.isOnline();
            if (online) {
                redisBroadcast.pushDevice(reminder.getDeviceId(), reminder.getContent(), null);
                reminderService.markFired(reminder, now);
            } else {
                reminderService.markMissed(reminder, now);
            }
        }
        log.info("提醒扫描完成：到点 {} 条", due.size());
    }
}
