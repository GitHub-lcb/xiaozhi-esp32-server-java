package com.xiaozhi.memorygraph.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryEntityDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryEntityMapper extends BaseMapper<MemoryEntityDO> {
}
