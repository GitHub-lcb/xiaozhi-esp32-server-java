package com.xiaozhi.stackchan.app.model.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」保存角色配置的请求体，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §1 的 PUT /role。
 * <p>
 * 所有字段可选，只更新传了的。**刻意没有 roleId 字段**：角色一律取当前用户的默认角色，
 * 从协议上就不给越权改别人角色的入口；App 即使传了 roleId 也会被 Jackson 忽略。
 * <p>
 * 范围校验不在这里用 jakarta validation 注解，而是由 AppService 手工抛
 * {@link com.xiaozhi.stackchan.app.StackChanAppException#business(String)}，
 * 这样错误响应仍是 App 认识的 {@code {"code":300,...}} 结构。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "App 保存 AI 角色配置")
public class StackChanAiRoleUpdateReq {

    @Schema(description = "角色名称，1~20 字符")
    private String roleName;

    @Schema(description = "音色名称")
    private String voiceName;

    @Schema(description = "语音音调，0.5~2")
    private Double ttsPitch;

    @Schema(description = "语音语速，0.5~2")
    private Double ttsSpeed;

    @Schema(description = "温度参数，0~1")
    private Double temperature;

    @Schema(description = "Top-P参数，0~1")
    private Double topP;

    @Schema(description = "TTS 配置ID，非正数表示服务端本地音色")
    private Integer ttsId;

    @Schema(description = "模型配置ID")
    private Integer modelId;

    @Schema(description = "STT 配置ID，非正数表示服务端本地识别")
    private Integer sttId;

    @Schema(description = "VAD 静音毫秒数，100~3000")
    private Integer vadSilenceMs;

    @Schema(description = "会话空闲自动结束秒数，10~600")
    private Integer inactiveTimeoutSeconds;
}
