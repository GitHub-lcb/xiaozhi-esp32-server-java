package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」可选语音识别项，字段对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §2。
 * <p>
 * 来源是当前用户 {@code sys_config} 里的 stt 配置，只给 id / 名称，不含任何密钥。
 */
@Data
@Schema(description = "App 可选语音识别项")
public class StackChanAiSttOptionResp {

    @Schema(description = "STT 配置ID")
    private Integer sttId;

    @Schema(description = "识别引擎名称（sys_config.configName）")
    private String name;

    @Schema(description = "下拉展示名")
    private String label;
}
