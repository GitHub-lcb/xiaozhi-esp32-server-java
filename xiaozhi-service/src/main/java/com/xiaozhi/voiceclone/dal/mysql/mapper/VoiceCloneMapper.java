package com.xiaozhi.voiceclone.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.voiceclone.dal.mysql.dataobject.VoiceCloneDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoiceCloneMapper extends BaseMapper<VoiceCloneDO> {
}
