package com.xiaozhi.voiceclone.service.impl;

import com.xiaozhi.common.model.bo.VoiceCloneBO;
import com.xiaozhi.voiceclone.convert.VoiceCloneConvert;
import com.xiaozhi.voiceclone.dal.mysql.dataobject.VoiceCloneDO;
import com.xiaozhi.voiceclone.dal.mysql.mapper.VoiceCloneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 克隆记录的状态机：创建即「处理中」，成功写 voiceId，失败写原因——不能出现既就绪又没 voiceId 的记录。
 */
@ExtendWith(MockitoExtension.class)
class VoiceCloneServiceImplTest {

    @Mock
    private VoiceCloneMapper voiceCloneMapper;

    @InjectMocks
    private VoiceCloneServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "voiceCloneConvert", Mappers.getMapper(VoiceCloneConvert.class));
    }

    @Test
    void createStartsInProcessingState() {
        service.create(bo());

        ArgumentCaptor<VoiceCloneDO> captor = ArgumentCaptor.forClass(VoiceCloneDO.class);
        verify(voiceCloneMapper).insert(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(VoiceCloneBO.STATE_PROCESSING);
    }

    @Test
    void successWritesVoiceIdAndClearsError() {
        when(voiceCloneMapper.selectById(1L)).thenReturn(existing());

        service.markResult(1L, "cosyvoice-v3-abc", null);

        ArgumentCaptor<VoiceCloneDO> captor = ArgumentCaptor.forClass(VoiceCloneDO.class);
        verify(voiceCloneMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(VoiceCloneBO.STATE_READY);
        assertThat(captor.getValue().getVoiceId()).isEqualTo("cosyvoice-v3-abc");
        assertThat(captor.getValue().getErrorMsg()).isNull();
    }

    @Test
    void failureKeepsReason() {
        when(voiceCloneMapper.selectById(1L)).thenReturn(existing());

        service.markResult(1L, null, "服务商返回 403：未开通克隆能力");

        ArgumentCaptor<VoiceCloneDO> captor = ArgumentCaptor.forClass(VoiceCloneDO.class);
        verify(voiceCloneMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(VoiceCloneBO.STATE_FAILED);
        assertThat(captor.getValue().getErrorMsg()).contains("未开通");
    }

    private VoiceCloneBO bo() {
        VoiceCloneBO bo = new VoiceCloneBO();
        bo.setUserId(1);
        bo.setVoiceName("我的声音");
        bo.setProvider("aliyun");
        bo.setSamplePath("voice-clone/a.wav");
        return bo;
    }

    private VoiceCloneDO existing() {
        VoiceCloneDO dataObject = new VoiceCloneDO();
        dataObject.setCloneId(1L);
        dataObject.setUserId(1);
        dataObject.setState(VoiceCloneBO.STATE_PROCESSING);
        return dataObject;
    }
}
