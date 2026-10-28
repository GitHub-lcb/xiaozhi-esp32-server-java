package com.xiaozhi.firmware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.firmware.convert.FirmwareConvert;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareDO;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareReleaseDO;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareMapper;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareReleaseMapper;
import com.xiaozhi.firmware.service.FirmwareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 固件版本库。版本号 + 芯片型号唯一：同一颗芯片下不能有两个 1.2.3。
 */
@Service
public class FirmwareServiceImpl implements FirmwareService {

    @Resource
    private FirmwareMapper firmwareMapper;

    @Resource
    private FirmwareReleaseMapper firmwareReleaseMapper;

    @Resource
    private FirmwareConvert firmwareConvert;

    @Override
    public PageResult<FirmwareBO> page(int pageNo, int pageSize, String version, String chipModel, String state) {
        LambdaQueryWrapper<FirmwareDO> query = Wrappers.<FirmwareDO>lambdaQuery()
            .like(StringUtils.hasText(version), FirmwareDO::getVersion, version)
            .eq(StringUtils.hasText(chipModel), FirmwareDO::getChipModel, chipModel)
            .eq(StringUtils.hasText(state), FirmwareDO::getState, state)
            .orderByDesc(FirmwareDO::getFirmwareId);
        IPage<FirmwareDO> result = firmwareMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<FirmwareBO> list = result.getRecords().stream().map(firmwareConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public FirmwareBO getBO(Integer firmwareId) {
        return firmwareConvert.toBO(requireFirmware(firmwareId));
    }

    @Override
    public List<FirmwareBO> listByIds(Collection<Integer> firmwareIds) {
        if (firmwareIds == null || firmwareIds.isEmpty()) {
            return List.of();
        }
        return firmwareMapper.selectBatchIds(firmwareIds).stream().map(firmwareConvert::toBO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FirmwareBO create(FirmwareBO bo) {
        String chipModel = normalizeChipModel(bo.getChipModel());
        assertVersionUnique(bo.getVersion(), chipModel, null);
        FirmwareDO dataObject = firmwareConvert.toDO(bo);
        dataObject.setChipModel(chipModel);
        if (!StringUtils.hasText(dataObject.getState())) {
            dataObject.setState(FirmwareBO.STATE_ENABLED);
        }
        firmwareMapper.insert(dataObject);
        return firmwareConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FirmwareBO update(Integer firmwareId, FirmwareBO bo) {
        FirmwareDO dataObject = requireFirmware(firmwareId);
        String chipModel = bo.getChipModel() == null ? dataObject.getChipModel() : normalizeChipModel(bo.getChipModel());
        String version = StringUtils.hasText(bo.getVersion()) ? bo.getVersion() : dataObject.getVersion();
        if (!Objects.equals(version, dataObject.getVersion()) || !Objects.equals(chipModel, dataObject.getChipModel())) {
            assertVersionUnique(version, chipModel, firmwareId);
        }
        firmwareConvert.updateDO(bo, dataObject);
        dataObject.setFirmwareId(firmwareId);
        dataObject.setChipModel(chipModel);
        dataObject.setVersion(version);
        firmwareMapper.updateById(dataObject);
        return firmwareConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer firmwareId) {
        requireFirmware(firmwareId);
        Long referenced = firmwareReleaseMapper.selectCount(
            Wrappers.<FirmwareReleaseDO>lambdaQuery().eq(FirmwareReleaseDO::getFirmwareId, firmwareId));
        if (referenced != null && referenced > 0) {
            throw new OperationFailedException("该固件已被 " + referenced + " 个升级任务引用，先删除任务再删固件");
        }
        firmwareMapper.deleteById(firmwareId);
    }

    private FirmwareDO requireFirmware(Integer firmwareId) {
        FirmwareDO dataObject = firmwareId == null ? null : firmwareMapper.selectById(firmwareId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("固件不存在");
        }
        return dataObject;
    }

    /**
     * 未填芯片型号表示「所有芯片通用」，统一存 NULL，否则唯一索引会被空串和 NULL 各占一份。
     */
    private String normalizeChipModel(String chipModel) {
        return StringUtils.hasText(chipModel) ? chipModel.trim() : null;
    }

    private void assertVersionUnique(String version, String chipModel, Integer excludeId) {
        List<FirmwareDO> sameVersion = firmwareMapper.selectList(
            Wrappers.<FirmwareDO>lambdaQuery().eq(FirmwareDO::getVersion, version));
        boolean duplicated = sameVersion.stream()
            .filter(item -> excludeId == null || !excludeId.equals(item.getFirmwareId()))
            .anyMatch(item -> Objects.equals(item.getChipModel(), chipModel));
        if (duplicated) {
            throw new OperationFailedException("该芯片型号下已存在版本 " + version);
        }
    }
}
