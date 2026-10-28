package com.xiaozhi.ai.voiceprint;

import java.util.List;

/**
 * 说话人向量提取。抽象出来是为了让测试与未装模型的环境都能跑：
 * 模型不在时 {@link #isAvailable()} 返回 false，接口直接报「未配置模型」而不是静默返回空向量。
 */
public interface SpeakerEmbeddingProvider {

    boolean isAvailable();

    /** 向量维度，未就绪时返回 0 */
    int dimension();

    /**
     * @param samples    16k 单声道 PCM 浮点样本
     * @param sampleRate 采样率
     */
    List<Double> embed(float[] samples, int sampleRate);
}
