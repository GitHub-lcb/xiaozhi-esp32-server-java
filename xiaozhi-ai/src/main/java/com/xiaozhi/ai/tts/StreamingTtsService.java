package com.xiaozhi.ai.tts;

import reactor.core.publisher.Flux;

/**
 * 支持音频级流式的 TTS 能力。
 * <p>
 * 现有 {@link TtsService#textToSpeech(String)} 只能返回整段音频文件：调用方必须等整句合成完、
 * 写完文件才能开始编码下发，首包延迟 = 整句合成时延。
 * 实现本接口的 provider 可以边合成边吐 PCM 分块，调用方拿到第一块就能编码播放。
 * <p>
 * 注意：分块边界音频不保证按帧对齐，调用方必须自行缓冲（{@code OpusProcessor} 已支持残帧）。
 */
public interface StreamingTtsService extends TtsService {

    /**
     * 边合成边返回 PCM 分块（16k / 单声道 / 16bit，与 {@link #textToSpeech(String)} 的产物同一格式）。
     *
     * @param text 一句完整的待合成文本
     */
    Flux<byte[]> synthesizeStream(String text);

    /**
     * 是否真的边合成边下发。
     * <p>
     * 返回 false 表示这是「伪流式」适配器（先拿到完整文件再分块吐），链路统一但首包不会变快。
     * 上层据此决定是否值得为它开启流式分支。
     */
    default boolean nativeStreaming() {
        return true;
    }
}
