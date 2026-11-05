package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」对话消息项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §4。
 * <p>
 * 库里的 {@code sys_message.sender} 在这里改名成契约里的 {@code role}，
 * {@code sys_message.message} 改名成 {@code content}。
 */
@Data
@Schema(description = "App 对话消息项")
public class StackChanAiMessageResp {

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "角色：user / assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间，毫秒时间戳")
    private Long createTime;
}
