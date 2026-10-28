package com.xiaozhi.command.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.command.dal.mysql.dataobject.DeviceCommandDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceCommandMapper extends BaseMapper<DeviceCommandDO> {
}
