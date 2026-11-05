package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * App「AI 设置」可选模型 / 音色 / 识别引擎清单，结构对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §2。
 * <p>
 * 任一列表没有可选项时返回空数组，不返回 null、也不报错。
 */
@Data
@Schema(description = "App AI 可选配置清单")
public class StackChanAiOptionsResp {

    @Schema(description = "可选模型")
    private List<StackChanAiModelOptionResp> models = new ArrayList<>();

    @Schema(description = "可选音色")
    private List<StackChanAiVoiceOptionResp> voices = new ArrayList<>();

    @Schema(description = "可选语音识别引擎")
    private List<StackChanAiSttOptionResp> stts = new ArrayList<>();
}
