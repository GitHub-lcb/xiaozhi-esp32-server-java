package com.xiaozhi.ai.coordination;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 同空间多设备就近应答：一组设备里谁先唤醒谁应答，其它设备在窗口期内保持沉默。
 * <p>
 * 用 Redis 的 SET NX EX 做「第一个唤醒者」的占位：竞态下只有一个能写成功，
 * 天然是原子的，不需要额外锁。窗口期短（默认 5 秒），过期后另一台设备可以正常应答。
 */
@Slf4j
@Component
public class WakeCoordinator {

    private static final String KEY_PREFIX = "xiaozhi:wake:";

    @Value("${xiaozhi.dialogue.wake-window-seconds:5}")
    private int windowSeconds = 5;

    private final StringRedisTemplate redisTemplate;

    public WakeCoordinator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试成为本组的应答者。
     *
     * @return true 表示由本设备应答；false 表示窗口期内已有别的设备应答，本设备应保持沉默
     */
    public boolean tryAcquire(Long groupId, String deviceId) {
        if (groupId == null || deviceId == null) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + groupId, deviceId, Duration.ofSeconds(windowSeconds));
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
            String holder = redisTemplate.opsForValue().get(KEY_PREFIX + groupId);
            // 同一台设备重复唤醒不算冲突；窗口内已由别的设备占位才需要沉默
            return deviceId.equals(holder);
        } catch (RuntimeException e) {
            // Redis 不可用时不做协同，宁可多台一起应答，也不能让设备叫不醒
            log.warn("唤醒协同失败，按单设备模式处理 groupId={}", groupId, e);
            return true;
        }
    }
}