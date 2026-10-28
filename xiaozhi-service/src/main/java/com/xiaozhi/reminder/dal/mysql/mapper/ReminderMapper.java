package com.xiaozhi.reminder.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.reminder.dal.mysql.dataobject.ReminderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderMapper extends BaseMapper<ReminderDO> {
}
