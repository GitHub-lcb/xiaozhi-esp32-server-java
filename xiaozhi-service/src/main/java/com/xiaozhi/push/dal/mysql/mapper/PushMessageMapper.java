package com.xiaozhi.push.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.push.dal.mysql.dataobject.PushMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PushMessageMapper extends BaseMapper<PushMessageDO> {
}
