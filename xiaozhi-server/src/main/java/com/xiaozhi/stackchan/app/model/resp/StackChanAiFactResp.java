package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」长期记忆事实项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §3。
 */
@Data
@Schema(description = "App 记忆事实项")
public class StackChanAiFactResp {

    @Schema(description = "事实ID")
    private Long factId;

    @Schema(description = "事实内容")
    private String content;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "来源：manual / auto")
    private String source;

    @Schema(description = "创建时间，毫秒时间戳")
    private Long createTime;
}
