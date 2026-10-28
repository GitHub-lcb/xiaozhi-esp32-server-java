package com.xiaozhi.ai.tts.clone;

/**
 * 服务商侧的「音色克隆」接口。
 * <p>
 * 各家（阿里 CosyVoice、火山）都需要：一段可被服务商访问的音频 + 一个前缀名，返回音色ID。
 * 抽象出来是为了让没开通克隆能力的环境明确失败，而不是静默产出一个假 voiceId。
 */
public interface VoiceCloneClient {

    /** 服务商标识，如 aliyun / volcengine */
    String provider();

    /** 当前环境是否可用（SDK 是否配置了密钥等） */
    boolean available();

    /**
     * @param voiceName 音色前缀，服务商侧会拼成唯一名字
     * @param audioUrl  样本音频的公网可访问地址
     * @return 服务商返回的音色ID
     */
    String enroll(String voiceName, String audioUrl);
}
