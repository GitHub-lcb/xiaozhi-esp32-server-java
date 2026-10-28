package com.xiaozhi.ai.voiceprint;

import com.xiaozhi.common.model.bo.VoiceprintBO;
import com.xiaozhi.voiceprint.service.VoiceprintService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 对话里的说话人识别：拿本轮音频去比对已注册的声纹。
 * <p>
 * 这是「锦上添花」的能力，任何一步失败都必须安静地返回空——
 * 模型没装、用户没注册、音频读不出来，都不能影响这一轮对话。
 */
@Slf4j
@Component
public class SpeakerIdentifier {

    @Value("${xiaozhi.voiceprint.threshold:0.62}")
    private double threshold = VoiceprintMatcher.DEFAULT_THRESHOLD;

    @Resource
    private VoiceprintService voiceprintService;

    @Resource
    private SpeakerEmbeddingProvider speakerEmbeddingProvider;

    public Optional<String> identify(Integer userId, float[] samples) {
        if (userId == null || samples == null || samples.length == 0) {
            return Optional.empty();
        }
        try {
            if (!speakerEmbeddingProvider.isAvailable()) {
                return Optional.empty();
            }
            List<VoiceprintBO> candidates = voiceprintService.listEnabled(userId);
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            return VoiceprintMatcher.best(speakerEmbeddingProvider.embed(samples, 16000), candidates, threshold)
                .filter(VoiceprintMatcher.Match::matched)
                .map(match -> match.voiceprint().getMemberName());
        } catch (RuntimeException e) {
            log.debug("说话人识别失败，本轮按未知说话人处理", e);
            return Optional.empty();
        }
    }
}
