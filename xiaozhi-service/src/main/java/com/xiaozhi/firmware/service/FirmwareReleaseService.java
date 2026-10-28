package com.xiaozhi.firmware.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareReleaseBO;

import java.util.Optional;

/**
 * 固件升级任务的读写与「给这台设备下发哪个版本」的决策。
 */
public interface FirmwareReleaseService {

    PageResult<FirmwareReleaseBO> page(int pageNo, int pageSize, String releaseName, Integer firmwareId, String state);

    FirmwareReleaseBO getBO(Integer releaseId);

    FirmwareReleaseBO create(FirmwareReleaseBO bo);

    FirmwareReleaseBO update(Integer releaseId, FirmwareReleaseBO bo);

    /**
     * 切换任务状态：1 进行中 / 2 已暂停 / 3 已结束。
     */
    FirmwareReleaseBO changeState(Integer releaseId, String state);

    void delete(Integer releaseId);

    /**
     * 设备 OTA 时判定是否有可下发的升级。
     *
     * @param deviceId       设备ID（已归一化）
     * @param chipModel      设备上报的芯片型号
     * @param currentVersion 设备当前固件版本
     * @return 命中的升级任务，没有则空
     */
    Optional<FirmwareReleaseBO> resolveForDevice(String deviceId, String chipModel, String currentVersion);
}
