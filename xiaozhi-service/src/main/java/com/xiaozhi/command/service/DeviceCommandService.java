package com.xiaozhi.command.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceCommandBO;

public interface DeviceCommandService {

    PageResult<DeviceCommandBO> page(int pageNo, int pageSize, String deviceId, String state);

    DeviceCommandBO record(DeviceCommandBO bo);

    /** 设备侧执行结果回填 */
    void markResult(Long commandId, boolean success, String result);

    DeviceCommandBO getBO(Long commandId);
}
