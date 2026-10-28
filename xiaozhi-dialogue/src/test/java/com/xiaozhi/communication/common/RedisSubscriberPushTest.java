package com.xiaozhi.communication.common;

import com.xiaozhi.dialogue.runtime.Persona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主动推送的落点：只有「设备在本实例上且 Persona 已就绪」才播报，
 * 其它情况静默跳过——server 按通道在线状态筛过目标，广播到了却没人处理是正常现象。
 */
class RedisSubscriberPushTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    private SessionManager sessionManager;
    private RedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        sessionManager = mock(SessionManager.class);
        subscriber = new RedisSubscriber();
        ReflectionTestUtils.setField(subscriber, "sessionManager", sessionManager);
    }

    @Test
    void speaksOnTheInstanceHoldingTheSession() {
        ChatSession session = mock(ChatSession.class);
        Persona persona = mock(Persona.class);
        when(sessionManager.getSessionByDeviceId(DEVICE_ID)).thenReturn(session);
        when(session.isAudioChannelOpen()).thenReturn(true);
        when(session.getPersona()).thenReturn(persona);

        subscriber.onDevicePush("{\"deviceId\":\"" + DEVICE_ID + "\",\"text\":\"该喝水了\",\"pushId\":1}");

        verify(persona).speakNow("该喝水了");
    }

    @Test
    void ignoresWhenDeviceIsNotOnThisInstance() {
        when(sessionManager.getSessionByDeviceId(DEVICE_ID)).thenReturn(null);

        subscriber.onDevicePush("{\"deviceId\":\"" + DEVICE_ID + "\",\"text\":\"该喝水了\",\"pushId\":1}");

        verify(sessionManager).getSessionByDeviceId(DEVICE_ID);
    }

    @Test
    void ignoresWhenPersonaIsNotReady() {
        ChatSession session = mock(ChatSession.class);
        when(sessionManager.getSessionByDeviceId(DEVICE_ID)).thenReturn(session);
        when(session.isAudioChannelOpen()).thenReturn(true);
        when(session.getPersona()).thenReturn(null);

        subscriber.onDevicePush("{\"deviceId\":\"" + DEVICE_ID + "\",\"text\":\"该喝水了\"}");

        verify(session, never()).sendTextMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void malformedMessageDoesNotThrow() {
        subscriber.onDevicePush("not-json");

        verify(sessionManager, never()).getSessionByDeviceId(org.mockito.ArgumentMatchers.anyString());
    }
}
