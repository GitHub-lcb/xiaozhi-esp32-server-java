package com.xiaozhi.reminder.service;

/**
 * 到点提醒扫描的执行入口（依赖倒置）。
 * <p>
 * 调度与多实例互斥放在 service 模块的 {@code com.xiaozhi.reminder.task.ReminderScanTask}：
 * server 编排层不允许直连 Redis（见 {@code ForbiddenDependencyArchTest}），而 Redisson 锁与
 * {@code @Scheduled} 在 service 模块的其它定时任务里已是既有范式。
 * <p>
 * 真正的业务编排与事务仍留在 server 的 {@code ReminderAppService}：任务只依赖本接口，
 * 运行时由 Spring 注入 server 的实现，锁会覆盖到事务提交之后才释放。
 */
public interface ReminderScanExecutor {

    /**
     * 扫描到点提醒：在线设备直接播报，离线跳过一次（重复任务顺延，一次性任务记为错过）。
     */
    void scanDueReminders();
}
