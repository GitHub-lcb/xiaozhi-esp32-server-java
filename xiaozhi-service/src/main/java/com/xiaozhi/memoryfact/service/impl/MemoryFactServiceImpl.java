package com.xiaozhi.memoryfact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.memoryfact.convert.MemoryFactConvert;
import com.xiaozhi.memoryfact.dal.mysql.dataobject.MemoryFactDO;
import com.xiaozhi.memoryfact.dal.mysql.mapper.MemoryFactMapper;
import com.xiaozhi.memoryfact.service.MemoryFactService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MemoryFactServiceImpl implements MemoryFactService {

    @Resource
    private MemoryFactMapper memoryFactMapper;

    @Resource
    private MemoryFactConvert memoryFactConvert;

    @Override
    public PageResult<MemoryFactBO> page(int pageNo, int pageSize, String content, String memberName, Integer userId) {
        LambdaQueryWrapper<MemoryFactDO> query = Wrappers.<MemoryFactDO>lambdaQuery()
            .eq(MemoryFactDO::getUserId, userId)
            .like(StringUtils.hasText(content), MemoryFactDO::getContent, content)
            .eq(StringUtils.hasText(memberName), MemoryFactDO::getMemberName, memberName)
            .orderByDesc(MemoryFactDO::getFactId);
        IPage<MemoryFactDO> result = memoryFactMapper.selectPage(new Page<>(pageNo, pageSize), query);
        return new PageResult<>(result.getRecords().stream().map(memoryFactConvert::toBO).toList(),
            result.getTotal(), Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemoryFactBO create(MemoryFactBO bo) {
        MemoryFactDO dataObject = new MemoryFactDO();
        dataObject.setUserId(bo.getUserId());
        dataObject.setMemberName(StringUtils.hasText(bo.getMemberName()) ? bo.getMemberName().trim() : null);
        dataObject.setContent(bo.getContent().trim());
        dataObject.setCategory(bo.getCategory());
        dataObject.setSource(StringUtils.hasText(bo.getSource()) ? bo.getSource() : MemoryFactBO.SOURCE_MANUAL);
        dataObject.setState(MemoryFactBO.STATE_ENABLED);
        memoryFactMapper.insert(dataObject);
        return memoryFactConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long factId, Integer userId) {
        MemoryFactDO dataObject = factId == null ? null : memoryFactMapper.selectById(factId);
        if (dataObject == null || (userId != null && !userId.equals(dataObject.getUserId()))) {
            throw new ResourceNotFoundException("记忆不存在");
        }
        memoryFactMapper.deleteById(factId);
    }

    /**
     * 对话注入用：该说话人的记忆 + 全家的记忆，按创建时间倒序取最近若干条。
     */
    @Override
    public List<MemoryFactBO> listForPrompt(Integer userId, String memberName, int limit) {
        if (userId == null) {
            return List.of();
        }
        return memoryFactMapper.selectList(Wrappers.<MemoryFactDO>lambdaQuery()
                .eq(MemoryFactDO::getUserId, userId)
                .eq(MemoryFactDO::getState, MemoryFactBO.STATE_ENABLED)
                .and(wrapper -> {
                    if (StringUtils.hasText(memberName)) {
                        wrapper.eq(MemoryFactDO::getMemberName, memberName).or().isNull(MemoryFactDO::getMemberName);
                    } else {
                        wrapper.isNull(MemoryFactDO::getMemberName);
                    }
                })
                .orderByDesc(MemoryFactDO::getFactId)
                .last("limit " + Math.max(1, limit)))
            .stream()
            .map(memoryFactConvert::toBO)
            .toList();
    }
}
