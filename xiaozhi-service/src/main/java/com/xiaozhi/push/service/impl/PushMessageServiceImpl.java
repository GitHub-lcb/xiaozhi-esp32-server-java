package com.xiaozhi.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.PushMessageBO;
import com.xiaozhi.push.convert.PushMessageConvert;
import com.xiaozhi.push.dal.mysql.dataobject.PushMessageDO;
import com.xiaozhi.push.dal.mysql.mapper.PushMessageMapper;
import com.xiaozhi.push.service.PushMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PushMessageServiceImpl implements PushMessageService {

    @Resource
    private PushMessageMapper pushMessageMapper;

    @Resource
    private PushMessageConvert pushMessageConvert;

    @Override
    public PageResult<PushMessageBO> page(int pageNo, int pageSize, String title, String state) {
        LambdaQueryWrapper<PushMessageDO> query = Wrappers.<PushMessageDO>lambdaQuery()
            .like(StringUtils.hasText(title), PushMessageDO::getTitle, title)
            .eq(StringUtils.hasText(state), PushMessageDO::getState, state)
            .orderByDesc(PushMessageDO::getPushId);
        IPage<PushMessageDO> result = pushMessageMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<PushMessageBO> list = result.getRecords().stream().map(pushMessageConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public PushMessageBO getBO(Long pushId) {
        return pushMessageConvert.toBO(require(pushId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PushMessageBO create(PushMessageBO bo) {
        PushMessageDO dataObject = pushMessageConvert.toDO(bo);
        dataObject.setState(PushMessageBO.STATE_PENDING);
        dataObject.setTotalCount(0);
        dataObject.setSentCount(0);
        dataObject.setOfflineCount(0);
        pushMessageMapper.insert(dataObject);
        return pushMessageConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PushMessageBO update(PushMessageBO bo) {
        PushMessageDO dataObject = require(bo.getPushId());
        if (PushMessageBO.STATE_SENT.equals(dataObject.getState())) {
            throw new OperationFailedException("已发送的推送不能再修改");
        }
        dataObject.setTitle(bo.getTitle());
        dataObject.setContent(bo.getContent());
        dataObject.setTargetType(bo.getTargetType());
        dataObject.setTargetValue(bo.getTargetValue());
        pushMessageMapper.updateById(dataObject);
        return pushMessageConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSent(Long pushId, int sentCount, int offlineCount, int totalCount) {
        PushMessageDO dataObject = require(pushId);
        dataObject.setState(PushMessageBO.STATE_SENT);
        dataObject.setSentCount(sentCount);
        dataObject.setOfflineCount(offlineCount);
        dataObject.setTotalCount(totalCount);
        dataObject.setSentTime(com.xiaozhi.utils.DateUtils.now());
        pushMessageMapper.updateById(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long pushId) {
        PushMessageDO dataObject = require(pushId);
        if (PushMessageBO.STATE_PENDING.equals(dataObject.getState())) {
            // 待发送的推送直接删掉即可
            pushMessageMapper.deleteById(pushId);
            return;
        }
        throw new OperationFailedException("只有待发送的推送可以删除");
    }

    private PushMessageDO require(Long pushId) {
        PushMessageDO dataObject = pushId == null ? null : pushMessageMapper.selectById(pushId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("推送不存在");
        }
        return dataObject;
    }
}
