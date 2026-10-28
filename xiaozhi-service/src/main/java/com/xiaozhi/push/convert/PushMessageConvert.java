package com.xiaozhi.push.convert;

import com.xiaozhi.common.model.bo.PushMessageBO;
import com.xiaozhi.push.dal.mysql.dataobject.PushMessageDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PushMessageConvert {

    PushMessageBO toBO(PushMessageDO dataObject);

    @Mapping(target = "pushId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    PushMessageDO toDO(PushMessageBO bo);

    com.xiaozhi.common.model.resp.PushMessageResp toResp(PushMessageBO bo);
}
