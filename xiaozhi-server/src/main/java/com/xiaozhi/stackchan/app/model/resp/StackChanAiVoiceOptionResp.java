package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」可选音色项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §2。
 * <p>
 * 来源是当前用户 {@code sys_config} 里的 tts 配置，只给 id / 名称 / 提供商，不含任何密钥。
 */
@Data
@Schema(description = "App 可选音色项")
public class StackChanAiVoiceOptionResp {

    @Schema(description = "TTS 配置ID")
    private Integer ttsId;

    @Schema(description = "音色名称（sys_config.configName）")
    private String name;

    @Schema(description = "服务提供商")
    private String provider;

    @Schema(description = "下拉展示名")
    private String label;
}
