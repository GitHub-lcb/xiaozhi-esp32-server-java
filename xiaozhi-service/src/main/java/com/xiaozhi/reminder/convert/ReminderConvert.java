package com.xiaozhi.reminder.convert;

import com.xiaozhi.common.model.bo.ReminderBO;
import com.xiaozhi.reminder.dal.mysql.dataobject.ReminderDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReminderConvert {

    ReminderBO toBO(ReminderDO dataObject);

    @Mapping(target = "reminderId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ReminderDO toDO(ReminderBO bo);

    com.xiaozhi.common.model.resp.ReminderResp toResp(ReminderBO bo);
}
