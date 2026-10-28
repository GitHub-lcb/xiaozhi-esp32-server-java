package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库：一组文档的集合，可绑定到角色，对话时自动检索。
 */
@Data
public class KnowledgeBaseBO {

    public static final String STATE_ENABLED = "1";
    public static final String STATE_DISABLED = "0";

    private Integer knowledgeId;
    private String name;
    private String description;
    private Integer roleId;
    private Integer embeddingConfigId;
    private String state;
    private Integer createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
