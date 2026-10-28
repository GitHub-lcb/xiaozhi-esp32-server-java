package com.xiaozhi.ai.voiceprint;

import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor;
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 sherpa-onnx 的说话人向量（如 3D-Speaker / WeSpeaker 模型）。
 * <p>
 * 模型文件不在就保持不可用状态，不阻塞启动：声纹是增强功能，缺模型时其它功能照常。
 */
@Slf4j
@Component
public class SherpaSpeakerEmbeddingProvider implements SpeakerEmbeddingProvider {

    @Value("${xiaozhi.voiceprint.model-path:models/speaker/speaker.onnx}")
    private String modelPath;

    @Value("${xiaozhi.voiceprint.num-threads:2}")
    private int numThreads = 2;

    private SpeakerEmbeddingExtractor extractor;
    private boolean initialized;

    @Override
    public boolean isAvailable() {
        initialize();
        return extractor != null;
    }

    @Override
    public int dimension() {
        initialize();
        return extractor == null ? 0 : extractor.getDim();
    }

    @Override
    public List<Double> embed(float[] samples, int sampleRate) {
        initialize();
        if (extractor == null) {
            throw new IllegalStateException("声纹模型未就绪：" + modelPath + " 不存在或加载失败");
        }
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("音频样本为空");
        }
        OnlineStream stream = extractor.createStream();
        try {
            stream.acceptWaveform(samples, sampleRate);
            stream.inputFinished();
            float[] vector = extractor.compute(stream);
            List<Double> values = new ArrayList<>(vector.length);
            for (float value : vector) {
                values.add((double) value);
            }
            return values;
        } finally {
            stream.release();
        }
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (modelPath == null || !Files.exists(Path.of(modelPath))) {
                log.info("声纹模型不存在，声纹识别不可用：{}", modelPath);
                return;
            }
            SpeakerEmbeddingExtractorConfig config = SpeakerEmbeddingExtractorConfig.builder()
                .setModel(modelPath)
                .setNumThreads(numThreads)
                .setDebug(false)
                .setProvider("cpu")
                .build();
            extractor = new SpeakerEmbeddingExtractor(config);
            log.info("声纹模型已加载：{}，维度={}", modelPath, extractor.getDim());
        } catch (RuntimeException e) {
            log.error("声纹模型加载失败，声纹识别不可用：{}", modelPath, e);
            extractor = null;
        }
    }

    @PreDestroy
    public void release() {
        if (extractor != null) {
            extractor.release();
        }
    }
}
