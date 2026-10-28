package com.xiaozhi.screenshot.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceScreenshotBO;

import java.util.Map;

public interface DeviceScreenshotService {

    PageResult<DeviceScreenshotBO> page(int pageNo, int pageSize, String deviceId, String state);

    /** 建一条待回传记录，返回带主键的 BO */
    DeviceScreenshotBO record(DeviceScreenshotBO bo);

    /** 按指令ID回填结果：能取到图片地址算成功，否则记失败 */
    void completeByCommand(Long commandId, Map<String, Object> payload, String errorMessage);
}
