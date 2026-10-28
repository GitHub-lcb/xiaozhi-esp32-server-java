package com.xiaozhi.channel.convert;

import com.xiaozhi.channel.dal.mysql.dataobject.DeviceChannelDO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceChannelConvert {

    DeviceChannelBO toBO(DeviceChannelDO dataObject);

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    DeviceChannelDO toDO(DeviceChannelBO bo);
}
