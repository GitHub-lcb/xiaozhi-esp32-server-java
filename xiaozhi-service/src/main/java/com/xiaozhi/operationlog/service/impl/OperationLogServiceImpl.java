package com.xiaozhi.operationlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.OperationLogBO;
import com.xiaozhi.operationlog.convert.OperationLogConvert;
import com.xiaozhi.operationlog.dal.mysql.dataobject.OperationLogDO;
import com.xiaozhi.operationlog.dal.mysql.mapper.OperationLogMapper;
import com.xiaozhi.operationlog.service.OperationLogService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private OperationLogConvert operationLogConvert;

    @Override
    @Async
    public void saveAsync(OperationLogBO operationLog) {
        try {
            operationLogMapper.insert(operationLogConvert.toDO(operationLog));
        } catch (Exception e) {
            log.error("保存操作日志失败: module={} operation={}", operationLog.getModule(), operationLog.getOperation(), e);
        }
    }

    @Override
    public PageResult<OperationLogBO> page(int pageNo, int pageSize, String module, String operation,
                                           Integer userId, Boolean success,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLogDO> query = Wrappers.<OperationLogDO>lambdaQuery()
            .like(StringUtils.hasText(module), OperationLogDO::getModule, module)
            .like(StringUtils.hasText(operation), OperationLogDO::getOperation, operation)
            .eq(userId != null, OperationLogDO::getUserId, userId)
            .eq(success != null, OperationLogDO::getSuccess, success)
            .ge(startTime != null, OperationLogDO::getCreateTime, startTime)
            .le(endTime != null, OperationLogDO::getCreateTime, endTime)
            .orderByDesc(OperationLogDO::getCreateTime)
            .orderByDesc(OperationLogDO::getId);
        IPage<OperationLogDO> result = operationLogMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<OperationLogBO> list = result.getRecords().stream().map(operationLogConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public int deleteExpired(int retentionDays, int batchSize) {
        LocalDateTime expireBefore = DateUtils.now().minusDays(retentionDays);
        int deleted = 0;
        while (true) {
            List<Long> ids = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLogDO>()
                    .select(OperationLogDO::getId)
                    .lt(OperationLogDO::getCreateTime, expireBefore)
                    .orderByAsc(OperationLogDO::getId)
                    .last("LIMIT " + batchSize))
                .stream()
                .map(OperationLogDO::getId)
                .toList();
            if (ids.isEmpty()) {
                break;
            }
            operationLogMapper.delete(new LambdaQueryWrapper<OperationLogDO>().in(OperationLogDO::getId, ids));
            deleted += ids.size();
        }
        return deleted;
    }
}
