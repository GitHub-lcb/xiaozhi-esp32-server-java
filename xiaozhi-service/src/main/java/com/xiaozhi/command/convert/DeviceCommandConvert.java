package com.xiaozhi.command.convert;

import com.xiaozhi.command.dal.mysql.dataobject.DeviceCommandDO;
import com.xiaozhi.common.model.bo.DeviceCommandBO;
import com.xiaozhi.common.model.resp.DeviceCommandResp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceCommandConvert {

    DeviceCommandBO toBO(DeviceCommandDO dataObject);

    @Mapping(target = "commandId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    DeviceCommandDO toDO(DeviceCommandBO bo);

    DeviceCommandResp toResp(DeviceCommandBO bo);
}
