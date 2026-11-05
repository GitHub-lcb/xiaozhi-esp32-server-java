package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * App「AI 设置」记忆页数据：某台设备的摘要 + 当前用户的长期记忆事实，
 * 结构对应 {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §3。
 * <p>
 * 摘要按 (deviceId, roleId) 查；事实表没有设备维度，按 userId 全量返回（契约允许的降级）。
 */
@Data
@Schema(description = "App 设备记忆")
public class StackChanAiMemoryResp {

    @Schema(description = "设备 MAC，12 位小写纯 hex；没有设备时为 null")
    private String deviceMac;

    @Schema(description = "设备绑定的角色ID；没有设备时为 null")
    private Integer roleId;

    @Schema(description = "记忆摘要，时间倒序")
    private List<StackChanAiSummaryResp> summaries = new ArrayList<>();

    @Schema(description = "长期记忆事实，时间倒序")
    private List<StackChanAiFactResp> facts = new ArrayList<>();
}
