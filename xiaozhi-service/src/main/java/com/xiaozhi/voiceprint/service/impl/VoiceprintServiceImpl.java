package com.xiaozhi.voiceprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.VoiceprintBO;
import com.xiaozhi.utils.JsonUtil;
import com.xiaozhi.voiceprint.convert.VoiceprintConvert;
import com.xiaozhi.voiceprint.dal.mysql.dataobject.VoiceprintDO;
import com.xiaozhi.voiceprint.dal.mysql.mapper.VoiceprintMapper;
import com.xiaozhi.voiceprint.service.VoiceprintService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VoiceprintServiceImpl implements VoiceprintService {

    @Resource
    private VoiceprintMapper voiceprintMapper;

    @Resource
    private VoiceprintConvert voiceprintConvert;

    @Override
    public List<VoiceprintBO> listByUser(Integer userId) {
        return voiceprintMapper.selectList(Wrappers.<VoiceprintDO>lambdaQuery()
                .eq(VoiceprintDO::getUserId, userId)
                .orderByDesc(VoiceprintDO::getVoiceprintId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceprintBO register(VoiceprintBO bo) {
        VoiceprintDO dataObject = new VoiceprintDO();
        dataObject.setUserId(bo.getUserId());
        dataObject.setMemberName(bo.getMemberName());
        dataObject.setEmbedding(JsonUtil.toJson(bo.getEmbedding()));
        dataObject.setSampleSeconds(bo.getSampleSeconds());
        dataObject.setState(StringUtils.hasText(bo.getState()) ? bo.getState() : VoiceprintBO.STATE_ENABLED);
        voiceprintMapper.insert(dataObject);
        return toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long voiceprintId, Integer userId) {
        VoiceprintDO dataObject = voiceprintId == null ? null : voiceprintMapper.selectById(voiceprintId);
        if (dataObject == null || (userId != null && !userId.equals(dataObject.getUserId()))) {
            throw new ResourceNotFoundException("声纹不存在");
        }
        voiceprintMapper.deleteById(voiceprintId);
    }

    @Override
    public List<VoiceprintBO> listEnabled(Integer userId) {
        return voiceprintMapper.selectList(Wrappers.<VoiceprintDO>lambdaQuery()
                .eq(VoiceprintDO::getUserId, userId)
                .eq(VoiceprintDO::getState, VoiceprintBO.STATE_ENABLED))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public Optional<VoiceprintBO> getBO(Long voiceprintId) {
        return Optional.ofNullable(voiceprintId)
            .map(voiceprintMapper::selectById)
            .map(this::toBO);
    }

    private VoiceprintBO toBO(VoiceprintDO dataObject) {
        VoiceprintBO bo = voiceprintConvert.toBO(dataObject);
        if (bo != null && StringUtils.hasText(dataObject.getEmbedding())) {
            try {
                bo.setEmbedding(JsonUtil.fromJson(dataObject.getEmbedding(), new TypeReference<List<Double>>() {}));
            } catch (RuntimeException e) {
                log.warn("声纹向量解析失败 voiceprintId={}", dataObject.getVoiceprintId());
            }
        }
        return bo;
    }
}
