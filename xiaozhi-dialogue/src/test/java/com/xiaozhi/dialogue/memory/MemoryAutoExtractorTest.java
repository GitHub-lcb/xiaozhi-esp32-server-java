package com.xiaozhi.dialogue.memory;

import com.xiaozhi.ai.memory.MemoryFactExtractor;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.event.ChatSessionClosedEvent;
import com.xiaozhi.message.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自动提炼的三道闸门：默认关闭、发言太少不提炼、设备缺角色不提炼——任一不满足都不能产生模型调用。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoryAutoExtractorTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private MemoryFactExtractor memoryFactExtractor;

    @Mock
    private DeviceService deviceService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MemoryAutoExtractor autoExtractor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(autoExtractor, "enabled", true);
        ReflectionTestUtils.setField(autoExtractor, "minTurns", 3);
        DeviceBO device = new DeviceBO();
        device.setDeviceId(DEVICE_ID);
        device.setUserId(1);
        device.setRoleId(2);
        when(deviceService.getBO(DEVICE_ID)).thenReturn(device);
        when(messageService.listHistory(anyString(), any(), anyInt())).thenReturn(history(3));
    }

    @Test
    void disabledSwitchMeansNoModelCall() {
        ReflectionTestUtils.setField(autoExtractor, "enabled", false);

        autoExtractor.onSessionClosed(event());

        verify(memoryFactExtractor, never()).extract(any(), any(), any(), any());
    }

    @Test
    void tooFewTurnsIsSkipped() {
        when(messageService.listHistory(anyString(), any(), anyInt())).thenReturn(history(1));

        autoExtractor.onSessionClosed(event());

        verify(memoryFactExtractor, never()).extract(any(), any(), any(), any());
    }

    @Test
    void enoughTurnsTriggersExtraction() {
        autoExtractor.onSessionClosed(event());

        verify(memoryFactExtractor).extract(1, DEVICE_ID, 2, null);
    }

    @Test
    void deviceWithoutRoleIsSkipped() {
        DeviceBO device = new DeviceBO();
        device.setDeviceId(DEVICE_ID);
        device.setUserId(1);
        when(deviceService.getBO(DEVICE_ID)).thenReturn(device);

        autoExtractor.onSessionClosed(event());

        verify(memoryFactExtractor, never()).extract(any(), any(), any(), any());
    }

    private ChatSessionClosedEvent event() {
        return new ChatSessionClosedEvent(this, "session-1", DEVICE_ID);
    }

    private List<MessageBO> history(int userTurns) {
        List<MessageBO> messages = new ArrayList<>();
        for (int i = 0; i < userTurns; i++) {
            MessageBO user = new MessageBO();
            user.setSender("user");
            user.setMessage("第" + i + "句");
            messages.add(user);
            MessageBO assistant = new MessageBO();
            assistant.setSender("assistant");
            assistant.setMessage("回复" + i);
            messages.add(assistant);
        }
        return messages;
    }
}
