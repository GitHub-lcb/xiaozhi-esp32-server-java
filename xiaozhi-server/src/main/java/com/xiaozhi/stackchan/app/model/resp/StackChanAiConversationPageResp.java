package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * App「AI 设置」对话记录分页结构，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §4：{@code {"total": n, "rows": [...]}}。
 * <p>
 * 刻意不复用管理端的 {@code PageResult}（它序列化成 list/total/pageNo/pageSize），
 * App 契约只认 total + rows。
 */
@Data
@Schema(description = "App 对话记录分页")
public class StackChanAiConversationPageResp {

    @Schema(description = "总记录数")
    private Long total = 0L;

    @Schema(description = "当前页会话")
    private List<StackChanAiConversationResp> rows = new ArrayList<>();
}
