package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档：上传后异步解析、切片、向量化。
 */
@Data
public class KnowledgeDocBO {

    public static final String STATE_PROCESSING = "0";
    public static final String STATE_READY = "1";
    public static final String STATE_FAILED = "2";

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
