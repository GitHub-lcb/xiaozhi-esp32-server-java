package com.xiaozhi.memoryfact.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.memoryfact.dal.mysql.dataobject.MemoryFactDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryFactMapper extends BaseMapper<MemoryFactDO> {
}
