package com.xiaozhi.knowledge.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge_doc")
public class KnowledgeDocDO extends BaseDO {

    @TableId(value = "docId", type = IdType.AUTO)
    private Long docId;

    private Integer knowledgeId;
    private String fileName;
    private String storedPath;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private String state;
    private String errorMsg;
    private Integer createUserId;
}
