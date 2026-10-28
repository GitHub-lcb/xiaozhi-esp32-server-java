package com.xiaozhi.knowledge.convert;

import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeDocBO;
import com.xiaozhi.common.model.resp.KnowledgeBaseResp;
import com.xiaozhi.common.model.resp.KnowledgeDocResp;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeBaseDO;
import com.xiaozhi.knowledge.dal.mysql.dataobject.KnowledgeDocDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface KnowledgeConvert {

    KnowledgeBaseBO toBO(KnowledgeBaseDO dataObject);

    @Mapping(target = "knowledgeId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    KnowledgeBaseDO toDO(KnowledgeBaseBO bo);

    KnowledgeBaseResp toResp(KnowledgeBaseBO bo);

    @Mapping(target = "knowledgeId", ignore = true)
    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "embeddingConfigId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    KnowledgeBaseBO toBO(com.xiaozhi.common.model.req.KnowledgeBaseSaveReq req);

    @Mapping(target = "knowledgeId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDO(KnowledgeBaseBO bo, @MappingTarget KnowledgeBaseDO dataObject);

    KnowledgeDocBO toBO(KnowledgeDocDO dataObject);

    @Mapping(target = "docId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    KnowledgeDocDO toDO(KnowledgeDocBO bo);

    KnowledgeDocResp toDocResp(KnowledgeDocBO bo);
}
