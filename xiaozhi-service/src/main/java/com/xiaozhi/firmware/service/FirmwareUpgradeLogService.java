package com.xiaozhi.firmware.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareUpgradeLogBO;

public interface FirmwareUpgradeLogService {

    PageResult<FirmwareUpgradeLogBO> page(int pageNo, int pageSize, String deviceId, Integer releaseId, String status);

    /** 记录一次下发。失败只记日志，不影响 OTA 主流程 */
    void record(FirmwareUpgradeLogBO bo);
}
