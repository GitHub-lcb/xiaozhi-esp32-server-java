package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "长期记忆事实")
public class MemoryFactResp {

    @Schema(description = "事实ID")
    private Long factId;

    @Schema(description = "说话人/成员，空表示全家")
    private String memberName;

    @Schema(description = "事实内容")
    private String content;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "来源：manual / auto")
    private String source;

    @Schema(description = "状态(1启用 0停用)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
