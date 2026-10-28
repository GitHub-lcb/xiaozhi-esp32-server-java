package com.xiaozhi.firmware.convert;

import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.common.model.req.FirmwareCreateReq;
import com.xiaozhi.common.model.req.FirmwareUpdateReq;
import com.xiaozhi.common.model.resp.FirmwareResp;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareDO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FirmwareConvert {

    @Mapping(target = "firmwareId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareDO toDO(FirmwareBO bo);

    FirmwareBO toBO(FirmwareDO dataObject);

    @Mapping(target = "firmwareId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareBO toBO(FirmwareCreateReq req);

    @Mapping(target = "firmwareId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareBO toBO(FirmwareUpdateReq req);

    FirmwareResp toResp(FirmwareBO bo);

    @Mapping(target = "firmwareId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDO(FirmwareBO bo, @MappingTarget FirmwareDO dataObject);
}
