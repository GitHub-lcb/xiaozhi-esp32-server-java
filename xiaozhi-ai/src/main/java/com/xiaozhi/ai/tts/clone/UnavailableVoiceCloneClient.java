package com.xiaozhi.ai.tts.clone;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 未接入任何克隆服务商时的占位实现。
 * <p>
 * 明确报错优于假装成功：调用方拿到异常会写进克隆记录的 errorMsg，管理端能直接看到"没开通"。
 */
@Slf4j
@Component
public class UnavailableVoiceCloneClient implements VoiceCloneClient {

    @Override
    public String provider() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String enroll(String voiceName, String audioUrl) {
        throw new IllegalStateException("未接入音色克隆服务商：请在配置中提供阿里云/火山的克隆凭据，或使用已存在的克隆音色ID");
    }
}
