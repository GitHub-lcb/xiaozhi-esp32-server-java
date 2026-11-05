package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App「AI 设置」的默认角色配置，字段严格对应
 * {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md} §1。
 * <p>
 * 只下发 App 展示与编辑需要的字段，apiKey / baseUrl 等凭据一律不出现在本类里。
 * 时间字段统一毫秒时间戳，与 App 其它 v2 接口（joindate 等）一致。
 */
@Data
@Schema(description = "App AI 角色配置")
public class StackChanAiRoleResp {

    @Schema(description = "角色ID")
    private Integer roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色头像")
    private String avatar;

    @Schema(description = "音色名称")
    private String voiceName;

    @Schema(description = "语音音调")
    private Double ttsPitch;

    @Schema(description = "语音语速")
    private Double ttsSpeed;

    @Schema(description = "温度参数")
    private Double temperature;

    @Schema(description = "Top-P参数")
    private Double topP;

    @Schema(description = "TTS 配置ID，非正数表示服务端本地音色")
    private Integer ttsId;

    @Schema(description = "模型配置ID")
    private Integer modelId;

    @Schema(description = "STT 配置ID，非正数表示服务端本地识别")
    private Integer sttId;

    @Schema(description = "VAD 静音毫秒数")
    private Integer vadSilenceMs;

    @Schema(description = "会话空闲自动结束秒数")
    private Integer inactiveTimeoutSeconds;

    @Schema(description = "模型名称，取不到为 null")
    private String modelName;

    @Schema(description = "模型提供商，取不到为 null")
    private String modelProvider;

    @Schema(description = "TTS 提供商，取不到为 null")
    private String ttsProvider;

    @Schema(description = "该角色下第一台设备的 12 位小写纯 hex MAC，没有设备为 null")
    private String deviceMac;

    @Schema(description = "角色更新时间，毫秒时间戳")
    private Long updateTime;
}
