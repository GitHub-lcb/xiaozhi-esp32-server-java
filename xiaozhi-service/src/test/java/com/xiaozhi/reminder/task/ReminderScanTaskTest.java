package com.xiaozhi.reminder.task;

import com.xiaozhi.reminder.service.ReminderScanExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多实例互斥口径：抢到锁才扫描，抢不到直接跳过，异常路径也要释放锁。
 * 提醒扫描是 server 侧定时任务，多实例部署时若没有锁，同一条提醒会被广播多次。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderScanTaskTest {

    @Mock
    private ReminderScanExecutor reminderScanExecutor;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private ReminderScanTask reminderScanTask;

    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(ReminderScanTask.SCAN_LOCK)).thenReturn(lock);
    }

    @Test
    void scansWhenLockAcquired() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        reminderScanTask.scanDueReminders();

        verify(reminderScanExecutor).scanDueReminders();
        verify(lock).unlock();
    }

    @Test
    void skipsWhenAnotherInstanceHoldsLock() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);

        reminderScanTask.scanDueReminders();

        verify(reminderScanExecutor, never()).scanDueReminders();
        verify(lock, never()).unlock();
    }

    @Test
    void releasesLockWhenScanThrows() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalStateException("boom")).when(reminderScanExecutor).scanDueReminders();

        assertThatThrownBy(reminderScanTask::scanDueReminders).isInstanceOf(IllegalStateException.class);

        verify(lock).unlock();
    }

    @Test
    void keepsInterruptFlagWhenWaitingForLockIsInterrupted() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        reminderScanTask.scanDueReminders();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(reminderScanExecutor, never()).scanDueReminders();
        verify(lock, never()).unlock();
        // 清掉中断标记，避免污染同 JVM 的后续测试
        Thread.interrupted();
    }
}
