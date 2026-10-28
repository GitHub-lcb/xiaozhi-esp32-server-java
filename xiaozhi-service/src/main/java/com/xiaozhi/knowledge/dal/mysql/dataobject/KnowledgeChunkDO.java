package com.xiaozhi.knowledge.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge_chunk")
public class KnowledgeChunkDO extends BaseDO {

    @TableId(value = "chunkId", type = IdType.AUTO)
    private Long chunkId;

    private Long docId;
    private Integer knowledgeId;
    private Integer chunkIndex;
    private String content;
    private String embedding;
}
