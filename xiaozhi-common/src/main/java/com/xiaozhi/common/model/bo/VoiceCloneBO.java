package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 音色克隆：把一段样本音频注册成服务商侧的音色，之后可作为 TTS 音色使用。 */
@Data
public class VoiceCloneBO {

    public static final String STATE_PROCESSING = "0";
    public static final String STATE_READY = "1";
    public static final String STATE_FAILED = "2";

    private Long cloneId;
    private Integer userId;
    private String voiceName;
    private String provider;
    private String voiceId;
    private String samplePath;
    private String state;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
