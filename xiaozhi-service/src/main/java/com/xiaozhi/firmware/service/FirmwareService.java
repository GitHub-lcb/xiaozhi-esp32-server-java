package com.xiaozhi.firmware.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareBO;

import java.util.Collection;
import java.util.List;

public interface FirmwareService {

    PageResult<FirmwareBO> page(int pageNo, int pageSize, String version, String chipModel, String state);

    FirmwareBO getBO(Integer firmwareId);

    /** 批量取固件，供升级任务列表回填版本号 */
    List<FirmwareBO> listByIds(Collection<Integer> firmwareIds);

    FirmwareBO create(FirmwareBO bo);

    FirmwareBO update(Integer firmwareId, FirmwareBO bo);

    void delete(Integer firmwareId);
}
