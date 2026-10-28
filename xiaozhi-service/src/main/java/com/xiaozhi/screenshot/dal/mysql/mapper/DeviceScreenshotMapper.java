package com.xiaozhi.screenshot.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.screenshot.dal.mysql.dataobject.DeviceScreenshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceScreenshotMapper extends BaseMapper<DeviceScreenshotDO> {
}
