package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * App「AI 设置」消息分页结构，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §4：{@code {"total": n, "rows": [...]}}。
 */
@Data
@Schema(description = "App 对话消息分页")
public class StackChanAiMessagePageResp {

    @Schema(description = "总记录数")
    private Long total = 0L;

    @Schema(description = "当前页消息")
    private List<StackChanAiMessageResp> rows = new ArrayList<>();
}
