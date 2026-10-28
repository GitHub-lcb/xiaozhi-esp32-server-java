package com.xiaozhi.ai.coordination;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 协同的底线：同一台设备重复唤醒不影响自己；别的设备占位时必须沉默；Redis 挂了要放行而不是拦死。
 */
class WakeCoordinatorTest {

    private static final Long GROUP = 1L;

    @SuppressWarnings("unchecked")
    private WakeCoordinator coordinator(boolean acquired, String holder) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(acquired);
        when(ops.get(anyString())).thenReturn(holder);
        return new WakeCoordinator(template);
    }

    @Test
    void firstWakeWins() {
        assertThat(coordinator(true, null).tryAcquire(GROUP, "dev-a")).isTrue();
    }

    @Test
    void otherDeviceStaysSilentWithinWindow() {
        assertThat(coordinator(false, "dev-a").tryAcquire(GROUP, "dev-b")).isFalse();
    }

    @Test
    void sameDeviceCanWakeAgain() {
        assertThat(coordinator(false, "dev-a").tryAcquire(GROUP, "dev-a")).isTrue();
    }

    @Test
    void deviceWithoutGroupAlwaysAnswers() {
        assertThat(coordinator(true, null).tryAcquire(null, "dev-a")).isTrue();
    }

    @Test
    void redisFailureFailsOpen() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        assertThat(new WakeCoordinator(template).tryAcquire(GROUP, "dev-a")).isTrue();
    }
}