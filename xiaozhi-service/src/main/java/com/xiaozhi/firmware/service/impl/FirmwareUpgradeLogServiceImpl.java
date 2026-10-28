package com.xiaozhi.firmware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareUpgradeLogBO;
import com.xiaozhi.firmware.convert.FirmwareUpgradeLogConvert;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareUpgradeLogDO;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareUpgradeLogMapper;
import com.xiaozhi.firmware.service.FirmwareUpgradeLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class FirmwareUpgradeLogServiceImpl implements FirmwareUpgradeLogService {

    @Resource
    private FirmwareUpgradeLogMapper upgradeLogMapper;

    @Resource
    private FirmwareUpgradeLogConvert upgradeLogConvert;

    @Override
    public PageResult<FirmwareUpgradeLogBO> page(int pageNo, int pageSize, String deviceId, Integer releaseId,
                                                 String status) {
        LambdaQueryWrapper<FirmwareUpgradeLogDO> query = Wrappers.<FirmwareUpgradeLogDO>lambdaQuery()
            .like(StringUtils.hasText(deviceId), FirmwareUpgradeLogDO::getDeviceId, deviceId)
            .eq(releaseId != null, FirmwareUpgradeLogDO::getReleaseId, releaseId)
            .eq(StringUtils.hasText(status), FirmwareUpgradeLogDO::getStatus, status)
            .orderByDesc(FirmwareUpgradeLogDO::getLogId);
        IPage<FirmwareUpgradeLogDO> result = upgradeLogMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<FirmwareUpgradeLogBO> list = result.getRecords().stream().map(upgradeLogConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public void record(FirmwareUpgradeLogBO bo) {
        try {
            FirmwareUpgradeLogDO dataObject = upgradeLogConvert.toDO(bo);
            if (!StringUtils.hasText(dataObject.getStatus())) {
                dataObject.setStatus(FirmwareUpgradeLogBO.STATUS_DELIVERED);
            }
            upgradeLogMapper.insert(dataObject);
        } catch (RuntimeException e) {
            // 记录失败不能让设备 OTA 失败
            log.warn("写入固件升级记录失败, deviceId={}, releaseId={}", bo.getDeviceId(), bo.getReleaseId(), e);
        }
    }
}
