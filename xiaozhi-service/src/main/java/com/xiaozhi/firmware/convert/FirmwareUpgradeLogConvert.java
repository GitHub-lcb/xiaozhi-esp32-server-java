package com.xiaozhi.firmware.convert;

import com.xiaozhi.common.model.bo.FirmwareUpgradeLogBO;
import com.xiaozhi.common.model.resp.FirmwareUpgradeLogResp;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareUpgradeLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FirmwareUpgradeLogConvert {

    @Mapping(target = "logId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareUpgradeLogDO toDO(FirmwareUpgradeLogBO bo);

    FirmwareUpgradeLogBO toBO(FirmwareUpgradeLogDO dataObject);

    @Mapping(target = "releaseName", ignore = true)
    FirmwareUpgradeLogResp toResp(FirmwareUpgradeLogBO bo);
}
