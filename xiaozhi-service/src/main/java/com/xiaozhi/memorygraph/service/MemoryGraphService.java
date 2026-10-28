package com.xiaozhi.memorygraph.service;

import java.util.List;

/**
 * 记忆图谱的读写：实体按 (userId, name) 去重，关系按三元组去重。
 */
public interface MemoryGraphService {

    /** 返回实体ID；不存在则创建 */
    Long upsertEntity(Integer userId, String name, String type);

    /** 已有相同三元组时跳过 */
    void addRelation(Integer userId, Long fromEntityId, Long toEntityId, String relation);

    /** 以「妈妈 喜欢 乌龙茶」的形式返回该用户的全部关系，供对话注入 */
    List<String> listRelationStatements(Integer userId, int limit);

    /** 按实体名建关系：实体不存在时自动创建，供管理端手工录入 */
    void addRelationByNames(Integer userId, String fromName, String relation, String toName, String fromType, String toType);
}
