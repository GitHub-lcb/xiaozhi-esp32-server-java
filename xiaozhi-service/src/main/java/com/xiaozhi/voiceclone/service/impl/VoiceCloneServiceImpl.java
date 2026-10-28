package com.xiaozhi.voiceclone.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.VoiceCloneBO;
import com.xiaozhi.voiceclone.convert.VoiceCloneConvert;
import com.xiaozhi.voiceclone.dal.mysql.dataobject.VoiceCloneDO;
import com.xiaozhi.voiceclone.dal.mysql.mapper.VoiceCloneMapper;
import com.xiaozhi.voiceclone.service.VoiceCloneService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class VoiceCloneServiceImpl implements VoiceCloneService {

    @Resource
    private VoiceCloneMapper voiceCloneMapper;

    @Resource
    private VoiceCloneConvert voiceCloneConvert;

    @Override
    public List<VoiceCloneBO> listByUser(Integer userId) {
        return voiceCloneMapper.selectList(Wrappers.<VoiceCloneDO>lambdaQuery()
                .eq(VoiceCloneDO::getUserId, userId)
                .orderByDesc(VoiceCloneDO::getCloneId))
            .stream().map(voiceCloneConvert::toBO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceCloneBO create(VoiceCloneBO bo) {
        VoiceCloneDO dataObject = new VoiceCloneDO();
        dataObject.setUserId(bo.getUserId());
        dataObject.setVoiceName(bo.getVoiceName());
        dataObject.setProvider(bo.getProvider());
        dataObject.setSamplePath(bo.getSamplePath());
        dataObject.setState(VoiceCloneBO.STATE_PROCESSING);
        voiceCloneMapper.insert(dataObject);
        return voiceCloneConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markResult(Long cloneId, String voiceId, String errorMsg) {
        VoiceCloneDO dataObject = cloneId == null ? null : voiceCloneMapper.selectById(cloneId);
        if (dataObject == null) {
            return;
        }
        if (StringUtils.hasText(voiceId)) {
            dataObject.setState(VoiceCloneBO.STATE_READY);
            dataObject.setVoiceId(voiceId);
            dataObject.setErrorMsg(null);
        } else {
            dataObject.setState(VoiceCloneBO.STATE_FAILED);
            dataObject.setErrorMsg(errorMsg != null && errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        }
        voiceCloneMapper.updateById(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long cloneId, Integer userId) {
        VoiceCloneDO dataObject = cloneId == null ? null : voiceCloneMapper.selectById(cloneId);
        if (dataObject == null || (userId != null && !userId.equals(dataObject.getUserId()))) {
            throw new ResourceNotFoundException("克隆记录不存在");
        }
        voiceCloneMapper.deleteById(cloneId);
    }

    @Override
    public List<VoiceCloneBO> listReady(Integer userId) {
        return voiceCloneMapper.selectList(Wrappers.<VoiceCloneDO>lambdaQuery()
                .eq(VoiceCloneDO::getUserId, userId)
                .eq(VoiceCloneDO::getState, VoiceCloneBO.STATE_READY))
            .stream().map(voiceCloneConvert::toBO).toList();
    }
}
