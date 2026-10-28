package com.xiaozhi.firmware.convert;

import com.xiaozhi.common.model.bo.FirmwareReleaseBO;
import com.xiaozhi.common.model.req.FirmwareReleaseCreateReq;
import com.xiaozhi.common.model.req.FirmwareReleaseUpdateReq;
import com.xiaozhi.common.model.resp.FirmwareReleaseResp;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareReleaseDO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FirmwareReleaseConvert {

    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareReleaseDO toDO(FirmwareReleaseBO bo);

    FirmwareReleaseBO toBO(FirmwareReleaseDO dataObject);

    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareReleaseBO toBO(FirmwareReleaseCreateReq req);

    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FirmwareReleaseBO toBO(FirmwareReleaseUpdateReq req);

    @Mapping(target = "firmwareVersion", ignore = true)
    FirmwareReleaseResp toResp(FirmwareReleaseBO bo);

    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDO(FirmwareReleaseBO bo, @MappingTarget FirmwareReleaseDO dataObject);
}
