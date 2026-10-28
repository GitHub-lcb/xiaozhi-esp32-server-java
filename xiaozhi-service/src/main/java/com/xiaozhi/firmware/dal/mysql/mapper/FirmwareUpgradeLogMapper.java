package com.xiaozhi.firmware.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareUpgradeLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FirmwareUpgradeLogMapper extends BaseMapper<FirmwareUpgradeLogDO> {
}
