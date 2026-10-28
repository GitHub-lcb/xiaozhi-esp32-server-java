package com.xiaozhi.knowledge.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge_base")
public class KnowledgeBaseDO extends BaseDO {

    @TableId(value = "knowledgeId", type = IdType.AUTO)
    private Integer knowledgeId;

    private String name;
    private String description;
    private Integer roleId;
    private Integer embeddingConfigId;
    private String state;
    private Integer createUserId;
}
