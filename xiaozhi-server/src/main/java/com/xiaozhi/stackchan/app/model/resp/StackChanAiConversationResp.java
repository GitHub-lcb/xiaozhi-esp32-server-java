package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」对话记录会话项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §4。
 */
@Data
@Schema(description = "App 对话记录会话项")
public class StackChanAiConversationResp {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "消息条数")
    private Long messageCount;

    @Schema(description = "最后一条消息时间，毫秒时间戳")
    private Long lastMessageTime;
}
