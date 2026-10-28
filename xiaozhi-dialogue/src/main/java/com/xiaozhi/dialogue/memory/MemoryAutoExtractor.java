package com.xiaozhi.dialogue.memory;

import com.xiaozhi.ai.memory.MemoryFactExtractor;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.event.ChatSessionClosedEvent;
import com.xiaozhi.message.service.MessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话结束时自动提炼长期记忆。
 * <p>
 * 默认关闭：每次提炼都是一次真实的大模型调用，是否开启应该由部署方按成本和隐私要求决定。
 * 打开后也只处理「用户真的说了几句」的会话，避免每句闲聊都触发提炼。
 */
@Slf4j
@Component
public class MemoryAutoExtractor {

    @Value("${xiaozhi.memory.auto-extract-enabled:false}")
    private boolean enabled;

    /** 低于这个用户发言数就不提炼 */
    @Value("${xiaozhi.memory.auto-extract-min-turns:3}")
    private int minTurns = 3;

    @Resource
    private MemoryFactExtractor memoryFactExtractor;

    @Resource
    private DeviceService deviceService;

    @Resource
    private MessageService messageService;

    @Async
    @EventListener
    public void onSessionClosed(ChatSessionClosedEvent event) {
        if (!enabled || event == null || event.getDeviceId() == null) {
            return;
        }
        try {
            DeviceBO device = deviceService.getBO(event.getDeviceId());
            if (device == null || device.getUserId() == null || device.getRoleId() == null) {
                return;
            }
            List<MessageBO> history = messageService.listHistory(device.getDeviceId(), device.getRoleId(), 30);
            long userTurns = history == null ? 0 : history.stream()
                .filter(message -> message != null && !"assistant".equalsIgnoreCase(message.getSender()))
                .count();
            if (userTurns < minTurns) {
                log.debug("会话发言不足 {} 条，跳过记忆提炼 deviceId={}", minTurns, device.getDeviceId());
                return;
            }
            memoryFactExtractor.extract(device.getUserId(), device.getDeviceId(), device.getRoleId(), null);
        } catch (RuntimeException e) {
            log.warn("会话结束自动提炼记忆失败，忽略 deviceId={}", event.getDeviceId(), e);
        }
    }
}
