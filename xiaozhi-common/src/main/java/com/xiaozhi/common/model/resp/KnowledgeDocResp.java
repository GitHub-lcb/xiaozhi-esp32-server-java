package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库文档")
public class KnowledgeDocResp {

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "知识库ID")
    private Integer knowledgeId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "分片数")
    private Integer chunkCount;

    @Schema(description = "状态(0处理中 1就绪 2失败)")
    private String state;

    @Schema(description = "失败原因")
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}
