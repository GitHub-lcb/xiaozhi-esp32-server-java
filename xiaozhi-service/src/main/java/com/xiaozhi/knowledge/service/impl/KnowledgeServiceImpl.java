package com.xiaozhi.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.common.model.bo.KnowledgeDocBO;
import com.xiaozhi.knowledge.convert.KnowledgeConvert;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeBaseDO;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeChunkDO;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeDocDO;
import com.xiaozhi.knowledge.dal.mysql.mapper.KnowledgeBaseMapper;
import com.xiaozhi.knowledge.dal.mysql.mapper.KnowledgeChunkMapper;
import com.xiaozhi.knowledge.dal.mysql.mapper.KnowledgeDocMapper;
import com.xiaozhi.knowledge.service.KnowledgeService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocMapper knowledgeDocMapper;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private KnowledgeConvert knowledgeConvert;

    @Override
    public PageResult<KnowledgeBaseBO> page(int pageNo, int pageSize, String name) {
        IPage<KnowledgeBaseDO> result = knowledgeBaseMapper.selectPage(new Page<>(pageNo, pageSize),
            Wrappers.<KnowledgeBaseDO>lambdaQuery()
                .like(StringUtils.hasText(name), KnowledgeBaseDO::getName, name)
                .orderByDesc(KnowledgeBaseDO::getKnowledgeId));
        List<KnowledgeBaseBO> list = result.getRecords().stream().map(knowledgeConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public KnowledgeBaseBO getBO(Integer knowledgeId) {
        return knowledgeConvert.toBO(requireBase(knowledgeId));
    }

    @Override
    public List<KnowledgeBaseBO> listEnabled() {
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseDO>lambdaQuery()
                .eq(KnowledgeBaseDO::getState, KnowledgeBaseBO.STATE_ENABLED))
            .stream()
            .map(knowledgeConvert::toBO)
            .toList();
    }

    @Override
    public List<KnowledgeBaseBO> listByRole(Integer roleId) {
        if (roleId == null) {
            return List.of();
        }
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseDO>lambdaQuery()
                .eq(KnowledgeBaseDO::getRoleId, roleId)
                .eq(KnowledgeBaseDO::getState, KnowledgeBaseBO.STATE_ENABLED))
            .stream()
            .map(knowledgeConvert::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseBO create(KnowledgeBaseBO bo) {
        KnowledgeBaseDO dataObject = knowledgeConvert.toDO(bo);
        if (!StringUtils.hasText(dataObject.getState())) {
            dataObject.setState(KnowledgeBaseBO.STATE_ENABLED);
        }
        knowledgeBaseMapper.insert(dataObject);
        return knowledgeConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseBO update(Integer knowledgeId, KnowledgeBaseBO bo) {
        KnowledgeBaseDO dataObject = requireBase(knowledgeId);
        knowledgeConvert.updateDO(bo, dataObject);
        dataObject.setKnowledgeId(knowledgeId);
        knowledgeBaseMapper.updateById(dataObject);
        return knowledgeConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer knowledgeId) {
        requireBase(knowledgeId);
        Long docs = knowledgeDocMapper.selectCount(
            Wrappers.<KnowledgeDocDO>lambdaQuery().eq(KnowledgeDocDO::getKnowledgeId, knowledgeId));
        if (docs != null && docs > 0) {
            throw new OperationFailedException("知识库里还有 " + docs + " 个文档，先删文档再删知识库");
        }
        knowledgeBaseMapper.deleteById(knowledgeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocBO createDoc(KnowledgeDocBO bo) {
        KnowledgeDocDO dataObject = knowledgeConvert.toDO(bo);
        dataObject.setState(KnowledgeDocBO.STATE_PROCESSING);
        dataObject.setChunkCount(0);
        knowledgeDocMapper.insert(dataObject);
        return knowledgeConvert.toBO(dataObject);
    }

    @Override
    public List<KnowledgeDocBO> listDocs(Integer knowledgeId) {
        return knowledgeDocMapper.selectList(Wrappers.<KnowledgeDocDO>lambdaQuery()
                .eq(KnowledgeDocDO::getKnowledgeId, knowledgeId)
                .orderByDesc(KnowledgeDocDO::getDocId))
            .stream()
            .map(knowledgeConvert::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDocState(Long docId, String state, int chunkCount, String errorMsg) {
        KnowledgeDocDO dataObject = knowledgeDocMapper.selectById(docId);
        if (dataObject == null) {
            return;
        }
        dataObject.setState(state);
        dataObject.setChunkCount(chunkCount);
        dataObject.setErrorMsg(errorMsg != null && errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        knowledgeDocMapper.updateById(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long docId) {
        knowledgeChunkMapper.delete(Wrappers.<KnowledgeChunkDO>lambdaQuery().eq(KnowledgeChunkDO::getDocId, docId));
        knowledgeDocMapper.deleteById(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChunks(List<KnowledgeChunkBO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (KnowledgeChunkBO chunk : chunks) {
            KnowledgeChunkDO dataObject = new KnowledgeChunkDO();
            dataObject.setDocId(chunk.getDocId());
            dataObject.setKnowledgeId(chunk.getKnowledgeId());
            dataObject.setChunkIndex(chunk.getChunkIndex());
            dataObject.setContent(chunk.getContent());
            dataObject.setEmbedding(chunk.getEmbedding() == null ? null : JsonUtil.toJson(chunk.getEmbedding()));
            knowledgeChunkMapper.insert(dataObject);
        }
    }

    @Override
    public List<KnowledgeChunkBO> listChunksByKnowledge(Integer knowledgeId) {
        return toChunks(knowledgeChunkMapper.selectList(Wrappers.<KnowledgeChunkDO>lambdaQuery()
            .eq(KnowledgeChunkDO::getKnowledgeId, knowledgeId)));
    }

    @Override
    public List<KnowledgeChunkBO> listChunksByKnowledgeIds(List<Integer> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        return toChunks(knowledgeChunkMapper.selectList(Wrappers.<KnowledgeChunkDO>lambdaQuery()
            .in(KnowledgeChunkDO::getKnowledgeId, knowledgeIds)));
    }

    private List<KnowledgeChunkBO> toChunks(List<KnowledgeChunkDO> rows) {
        return rows.stream().map(row -> {
            KnowledgeChunkBO bo = new KnowledgeChunkBO();
            bo.setChunkId(row.getChunkId());
            bo.setDocId(row.getDocId());
            bo.setKnowledgeId(row.getKnowledgeId());
            bo.setChunkIndex(row.getChunkIndex());
            bo.setContent(row.getContent());
            bo.setEmbedding(parseEmbedding(row.getEmbedding()));
            return bo;
        }).toList();
    }

    private List<Double> parseEmbedding(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtil.fromJson(json, new com.fasterxml.jackson.core.type.TypeReference<List<Double>>() {});
        } catch (RuntimeException e) {
            log.warn("解析向量失败，按未向量化处理 doc chunk 数据异常");
            return null;
        }
    }

    private KnowledgeBaseDO requireBase(Integer knowledgeId) {
        KnowledgeBaseDO dataObject = knowledgeId == null ? null : knowledgeBaseMapper.selectById(knowledgeId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("知识库不存在");
        }
        return dataObject;
    }
}
