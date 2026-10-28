package com.xiaozhi.knowledge.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeDocDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDocDO> {
}
