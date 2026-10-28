package com.xiaozhi.command.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.command.convert.DeviceCommandConvert;
import com.xiaozhi.command.dal.mysql.dataobject.DeviceCommandDO;
import com.xiaozhi.command.dal.mysql.mapper.DeviceCommandMapper;
import com.xiaozhi.command.service.DeviceCommandService;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    @Resource
    private DeviceCommandMapper deviceCommandMapper;

    @Resource
    private DeviceCommandConvert deviceCommandConvert;

    @Override
    public PageResult<DeviceCommandBO> page(int pageNo, int pageSize, String deviceId, String state) {
        LambdaQueryWrapper<DeviceCommandDO> query = Wrappers.<DeviceCommandDO>lambdaQuery()
            .like(StringUtils.hasText(deviceId), DeviceCommandDO::getDeviceId, deviceId)
            .eq(StringUtils.hasText(state), DeviceCommandDO::getState, state)
            .orderByDesc(DeviceCommandDO::getCommandId);
        IPage<DeviceCommandDO> result = deviceCommandMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<DeviceCommandBO> list = result.getRecords().stream().map(deviceCommandConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceCommandBO record(DeviceCommandBO bo) {
        DeviceCommandDO dataObject = deviceCommandConvert.toDO(bo);
        dataObject.setState(DeviceCommandBO.STATE_SENT);
        deviceCommandMapper.insert(dataObject);
        return deviceCommandConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markResult(Long commandId, boolean success, String result) {
        DeviceCommandDO dataObject = commandId == null ? null : deviceCommandMapper.selectById(commandId);
        if (dataObject == null) {
            log.warn("回填指令结果时找不到记录 commandId={}", commandId);
            return;
        }
        dataObject.setState(success ? DeviceCommandBO.STATE_SUCCESS : DeviceCommandBO.STATE_FAILED);
        dataObject.setResult(result != null && result.length() > 500 ? result.substring(0, 500) : result);
        deviceCommandMapper.updateById(dataObject);
    }

    @Override
    public DeviceCommandBO getBO(Long commandId) {
        DeviceCommandDO dataObject = commandId == null ? null : deviceCommandMapper.selectById(commandId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("指令不存在");
        }
        return deviceCommandConvert.toBO(dataObject);
    }
}
