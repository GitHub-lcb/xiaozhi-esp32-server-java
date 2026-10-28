package com.xiaozhi.screenshot.convert;

import com.xiaozhi.common.model.bo.DeviceScreenshotBO;
import com.xiaozhi.common.model.resp.DeviceScreenshotResp;
import com.xiaozhi.screenshot.dal.mysql.dataobject.DeviceScreenshotDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceScreenshotConvert {

    DeviceScreenshotBO toBO(DeviceScreenshotDO dataObject);

    @Mapping(target = "shotId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    DeviceScreenshotDO toDO(DeviceScreenshotBO bo);

    DeviceScreenshotResp toResp(DeviceScreenshotBO bo);
}
