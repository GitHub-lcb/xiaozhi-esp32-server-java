package com.xiaozhi.memorygraph.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryRelationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryRelationMapper extends BaseMapper<MemoryRelationDO> {
}
