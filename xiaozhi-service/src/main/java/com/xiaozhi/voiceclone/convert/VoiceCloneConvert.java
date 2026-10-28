package com.xiaozhi.voiceclone.convert;

import com.xiaozhi.common.model.bo.VoiceCloneBO;
import com.xiaozhi.common.model.resp.VoiceCloneResp;
import com.xiaozhi.voiceclone.dal.mysql.dataobject.VoiceCloneDO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoiceCloneConvert {

    VoiceCloneBO toBO(VoiceCloneDO dataObject);

    VoiceCloneResp toResp(VoiceCloneBO bo);
}
