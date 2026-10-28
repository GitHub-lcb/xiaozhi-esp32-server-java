package com.xiaozhi.firmware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.common.model.bo.FirmwareReleaseBO;
import com.xiaozhi.firmware.convert.FirmwareReleaseConvert;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareDO;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareReleaseDO;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareMapper;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareReleaseMapper;
import com.xiaozhi.firmware.service.FirmwareReleaseService;
import com.xiaozhi.utils.DateUtils;
import com.xiaozhi.utils.VersionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 固件升级任务：任务的增删改查，以及 OTA 时「这台设备该不该升、升到哪个版本」的判定。
 */
@Slf4j
@Service
public class FirmwareReleaseServiceImpl implements FirmwareReleaseService {

    private static final String STRATEGY_ALL = FirmwareReleaseBO.STRATEGY_ALL;
    private static final String STRATEGY_PERCENT = FirmwareReleaseBO.STRATEGY_PERCENT;
    private static final String STRATEGY_WHITELIST = FirmwareReleaseBO.STRATEGY_WHITELIST;

    @Resource
    private FirmwareReleaseMapper releaseMapper;

    @Resource
    private FirmwareMapper firmwareMapper;

    @Resource
    private FirmwareReleaseConvert releaseConvert;

    @Override
    public PageResult<FirmwareReleaseBO> page(int pageNo, int pageSize, String releaseName, Integer firmwareId,
                                              String state) {
        LambdaQueryWrapper<FirmwareReleaseDO> query = Wrappers.<FirmwareReleaseDO>lambdaQuery()
            .like(StringUtils.hasText(releaseName), FirmwareReleaseDO::getReleaseName, releaseName)
            .eq(firmwareId != null, FirmwareReleaseDO::getFirmwareId, firmwareId)
            .eq(StringUtils.hasText(state), FirmwareReleaseDO::getState, state)
            .orderByDesc(FirmwareReleaseDO::getReleaseId);
        IPage<FirmwareReleaseDO> result = releaseMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<FirmwareReleaseBO> list = result.getRecords().stream().map(releaseConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    public FirmwareReleaseBO getBO(Integer releaseId) {
        return releaseConvert.toBO(requireRelease(releaseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FirmwareReleaseBO create(FirmwareReleaseBO bo) {
        requireFirmware(bo.getFirmwareId());
        String strategy = normalizeStrategy(bo.getStrategy());
        validateStrategy(strategy, bo.getPercent(), bo.getWhitelist());
        validateWindow(bo.getStartTime(), bo.getEndTime());
        FirmwareReleaseDO dataObject = releaseConvert.toDO(bo);
        dataObject.setStrategy(strategy);
        // 新建的任务默认待开始，要显式「发布」才会下发给设备
        dataObject.setState(FirmwareReleaseBO.STATE_PENDING);
        releaseMapper.insert(dataObject);
        return releaseConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FirmwareReleaseBO update(Integer releaseId, FirmwareReleaseBO bo) {
        FirmwareReleaseDO dataObject = requireRelease(releaseId);
        if (FirmwareReleaseBO.STATE_FINISHED.equals(dataObject.getState())) {
            throw new OperationFailedException("已结束的任务不能再修改");
        }
        if (bo.getFirmwareId() != null) {
            requireFirmware(bo.getFirmwareId());
        }
        String strategy = StringUtils.hasText(bo.getStrategy()) ? normalizeStrategy(bo.getStrategy()) : dataObject.getStrategy();
        Integer percent = bo.getPercent() != null ? bo.getPercent() : dataObject.getPercent();
        String whitelist = bo.getWhitelist() != null ? bo.getWhitelist() : dataObject.getWhitelist();
        LocalDateTime startTime = bo.getStartTime() != null ? bo.getStartTime() : dataObject.getStartTime();
        LocalDateTime endTime = bo.getEndTime() != null ? bo.getEndTime() : dataObject.getEndTime();
        validateStrategy(strategy, percent, whitelist);
        validateWindow(startTime, endTime);
        releaseConvert.updateDO(bo, dataObject);
        dataObject.setReleaseId(releaseId);
        dataObject.setStrategy(strategy);
        releaseMapper.updateById(dataObject);
        return releaseConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FirmwareReleaseBO changeState(Integer releaseId, String state) {
        FirmwareReleaseDO dataObject = requireRelease(releaseId);
        if (FirmwareReleaseBO.STATE_ACTIVE.equals(state)) {
            FirmwareDO firmware = requireFirmware(dataObject.getFirmwareId());
            if (!FirmwareBO.STATE_ENABLED.equals(firmware.getState())) {
                throw new OperationFailedException("目标固件已停用，先启用固件再发布任务");
            }
            validateStrategy(dataObject.getStrategy(), dataObject.getPercent(), dataObject.getWhitelist());
            validateWindow(dataObject.getStartTime(), dataObject.getEndTime());
        } else if (FirmwareReleaseBO.STATE_PAUSED.equals(state)) {
            if (!FirmwareReleaseBO.STATE_ACTIVE.equals(dataObject.getState())) {
                throw new OperationFailedException("只有进行中的任务可以暂停");
            }
        } else if (!FirmwareReleaseBO.STATE_FINISHED.equals(state)) {
            throw new OperationFailedException("不支持的目标状态：" + state);
        }
        dataObject.setState(state);
        releaseMapper.updateById(dataObject);
        return releaseConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer releaseId) {
        FirmwareReleaseDO dataObject = requireRelease(releaseId);
        if (FirmwareReleaseBO.STATE_ACTIVE.equals(dataObject.getState())) {
            throw new OperationFailedException("进行中的任务不能删除，先暂停");
        }
        releaseMapper.deleteById(releaseId);
    }

    @Override
    public Optional<FirmwareReleaseBO> resolveForDevice(String deviceId, String chipModel, String currentVersion) {
        if (!StringUtils.hasText(deviceId)) {
            return Optional.empty();
        }
        List<FirmwareReleaseDO> active = releaseMapper.selectList(
            Wrappers.<FirmwareReleaseDO>lambdaQuery().eq(FirmwareReleaseDO::getState, FirmwareReleaseBO.STATE_ACTIVE));
        if (active.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime now = DateUtils.now();
        Map<Integer, FirmwareDO> firmwareCache = new HashMap<>();
        FirmwareReleaseDO matched = null;
        String matchedVersion = null;
        for (FirmwareReleaseDO release : active) {
            if (!inWindow(release, now)) {
                continue;
            }
            FirmwareDO firmware = firmwareCache.computeIfAbsent(release.getFirmwareId(), firmwareMapper::selectById);
            if (firmware == null || !FirmwareBO.STATE_ENABLED.equals(firmware.getState())) {
                continue;
            }
            if (!chipMatches(firmware.getChipModel(), chipModel)) {
                continue;
            }
            if (!isUpgradeCandidate(firmware.getVersion(), currentVersion)) {
                continue;
            }
            if (!strategyMatches(release, deviceId)) {
                continue;
            }
            if (matched == null || VersionUtils.compareVersion(firmware.getVersion(), matchedVersion) > 0) {
                matched = release;
                matchedVersion = firmware.getVersion();
            }
        }
        return Optional.ofNullable(matched).map(releaseConvert::toBO);
    }

    /**
     * 时间窗：两头都可以不设，设了就按 [startTime, endTime] 闭区间判定。
     */
    private boolean inWindow(FirmwareReleaseDO release, LocalDateTime now) {
        if (release.getStartTime() != null && now.isBefore(release.getStartTime())) {
            return false;
        }
        return release.getEndTime() == null || !now.isAfter(release.getEndTime());
    }

    /**
     * 任务限定了芯片型号时，设备必须明确上报同型号；设备没上报芯片一律不放行，避免刷错砖。
     */
    private boolean chipMatches(String restrictedChip, String deviceChip) {
        if (!StringUtils.hasText(restrictedChip)) {
            return true;
        }
        return StringUtils.hasText(deviceChip) && restrictedChip.trim().equalsIgnoreCase(deviceChip.trim());
    }

    /**
     * 目标版本必须高于设备当前版本，相同或更低都不下发，否则设备每次 OTA 都会重刷同一个包。
     */
    private boolean isUpgradeCandidate(String targetVersion, String currentVersion) {
        if (!StringUtils.hasText(targetVersion)) {
            return false;
        }
        if (!StringUtils.hasText(currentVersion)) {
            return true;
        }
        return VersionUtils.compareVersion(targetVersion.trim(), currentVersion.trim()) > 0;
    }

    private boolean strategyMatches(FirmwareReleaseDO release, String deviceId) {
        String strategy = normalizeStrategy(release.getStrategy());
        return switch (strategy) {
            case STRATEGY_PERCENT -> {
                int percent = release.getPercent() == null ? 0 : release.getPercent();
                yield percent > 0 && Math.floorMod(deviceId.hashCode(), 100) < percent;
            }
            case STRATEGY_WHITELIST -> {
                if (!StringUtils.hasText(release.getWhitelist())) {
                    yield false;
                }
                yield Arrays.stream(release.getWhitelist().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .anyMatch(id -> id.equalsIgnoreCase(deviceId));
            }
            default -> true;
        };
    }

    private FirmwareReleaseDO requireRelease(Integer releaseId) {
        FirmwareReleaseDO dataObject = releaseId == null ? null : releaseMapper.selectById(releaseId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("升级任务不存在");
        }
        return dataObject;
    }

    private FirmwareDO requireFirmware(Integer firmwareId) {
        FirmwareDO firmware = firmwareId == null ? null : firmwareMapper.selectById(firmwareId);
        if (firmware == null) {
            throw new ResourceNotFoundException("目标固件不存在");
        }
        return firmware;
    }

    private String normalizeStrategy(String strategy) {
        if (!StringUtils.hasText(strategy)) {
            return STRATEGY_ALL;
        }
        String value = strategy.trim().toLowerCase();
        if (!Objects.equals(value, STRATEGY_ALL) && !Objects.equals(value, STRATEGY_PERCENT)
            && !Objects.equals(value, STRATEGY_WHITELIST)) {
            throw new OperationFailedException("不支持的投放策略：" + strategy);
        }
        return value;
    }

    private void validateStrategy(String strategy, Integer percent, String whitelist) {
        if (STRATEGY_PERCENT.equals(strategy)) {
            if (percent == null || percent < 1 || percent > 100) {
                throw new OperationFailedException("按比例投放时，灰度百分比必须在 1-100 之间");
            }
        }
        if (STRATEGY_WHITELIST.equals(strategy) && !StringUtils.hasText(whitelist)) {
            throw new OperationFailedException("按白名单投放时，白名单不能为空");
        }
    }

    private void validateWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new OperationFailedException("时间窗结束时间必须晚于开始时间");
        }
    }
}
