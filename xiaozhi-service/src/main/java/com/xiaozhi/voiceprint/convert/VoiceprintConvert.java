package com.xiaozhi.voiceprint.convert;

import com.xiaozhi.common.model.bo.VoiceprintBO;
import com.xiaozhi.common.model.resp.VoiceprintResp;
import com.xiaozhi.voiceprint.dal.mysql.dataobject.VoiceprintDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoiceprintConvert {

    /** 向量以 JSON 存库，转换由 Service 负责，这里跳过避免 MapStruct 猜 String↔List */
    @Mapping(target = "embedding", ignore = true)
    VoiceprintBO toBO(VoiceprintDO dataObject);

    VoiceprintResp toResp(VoiceprintBO bo);
}
