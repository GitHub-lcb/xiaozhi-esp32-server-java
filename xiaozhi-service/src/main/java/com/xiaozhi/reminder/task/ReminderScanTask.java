package com.xiaozhi.reminder.task;

import com.xiaozhi.reminder.service.ReminderScanExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 提醒扫描定时任务：只负责「定时 + 多实例互斥」，业务与事务在 {@link ReminderScanExecutor} 的实现里。
 * <p>
 * 为什么放在 service 模块：server 是编排层，不允许直接依赖 Redis（{@code ForbiddenDependencyArchTest}）；
 * service 模块的其它定时清理任务（录音/验证码/审计日志）同样直接使用 Redisson 锁，这里是既有范式。
 * <p>
 * 锁必须覆盖到事务提交之后：抢锁 → 调执行器（事务代理开启/提交事务）→ 方法返回 → finally 释放锁。
 * 抢不到锁的实例直接跳过本轮，不排队补跑。
 */
@Slf4j
@Component
public class ReminderScanTask {

    static final String SCAN_LOCK = "lock:task:reminder-scan";

    @Resource
    private ReminderScanExecutor reminderScanExecutor;

    @Resource
    private RedissonClient redissonClient;

    @Scheduled(fixedDelayString = "${xiaozhi.reminder.scan-interval-ms:30000}")
    public void scanDueReminders() {
        RLock lock = redissonClient.getLock(SCAN_LOCK);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                log.info("已有实例在执行提醒扫描，跳过本轮");
                return;
            }
            reminderScanExecutor.scanDueReminders();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待提醒扫描锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
