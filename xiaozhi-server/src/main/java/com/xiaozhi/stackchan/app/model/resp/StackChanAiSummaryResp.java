package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」记忆摘要项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §3。
 */
@Data
@Schema(description = "App 记忆摘要项")
public class StackChanAiSummaryResp {

    /**
     * 与既有 {@code SummaryResp} 同一口径：摘要表没有自增主键，用 createTime 的毫秒时间戳当 id。
     */
    @Schema(description = "摘要ID（createTime 毫秒时间戳）")
    private Long id;

    @Schema(description = "摘要内容")
    private String summary;

    @Schema(description = "创建时间，毫秒时间戳")
    private Long createTime;
}
