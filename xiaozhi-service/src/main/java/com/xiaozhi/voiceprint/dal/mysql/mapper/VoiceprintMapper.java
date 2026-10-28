package com.xiaozhi.voiceprint.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.voiceprint.dal.mysql.dataobject.VoiceprintDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoiceprintMapper extends BaseMapper<VoiceprintDO> {
}
