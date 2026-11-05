package com.xiaozhi.stackchan.app.model.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」重命名会话的请求体，对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §4 的 PATCH /conversations/{sessionId}。
 * <p>
 * 长度上限与既有 {@code ConversationRenameReq} 保持一致（100 字），校验同样由 AppService 手工做，
 * 保证错误响应是 App 认识的 {@code {"code":300,...}} 结构。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "App 重命名对话记录")
public class StackChanAiConversationRenameReq {

    @Schema(description = "新标题，1~100 字")
    private String title;
}
