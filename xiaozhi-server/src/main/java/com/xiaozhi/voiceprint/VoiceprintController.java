package com.xiaozhi.voiceprint;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.ai.voiceprint.SpeakerEmbeddingProvider;
import com.xiaozhi.ai.voiceprint.VoiceprintMatcher;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.bo.VoiceprintBO;
import com.xiaozhi.common.model.resp.VoiceprintMatchResp;
import com.xiaozhi.common.model.resp.VoiceprintResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.voiceprint.convert.VoiceprintConvert;
import com.xiaozhi.voiceprint.service.VoiceprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 声纹识别：注册家庭成员声纹、用一段音频测试能认出谁。
 */
@RestController
@RequestMapping("/api/voiceprint")
@Tag(name = "声纹识别", description = "家庭成员声纹注册与识别")
public class VoiceprintController extends BaseController {

    /** 注册样本最短时长，太短的特征不可靠 */
    private static final int MIN_SAMPLES = 16000;

    @Value("${xiaozhi.voiceprint.threshold:0.62}")
    private double threshold = VoiceprintMatcher.DEFAULT_THRESHOLD;

    @Resource
    private VoiceprintService voiceprintService;

    @Resource
    private VoiceprintConvert voiceprintConvert;

    @Resource
    private SpeakerEmbeddingProvider speakerEmbeddingProvider;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:voiceprint:api:list")
    @Operation(summary = "声纹列表", description = "当前账号下已注册的声纹")
    public ApiResponse<List<VoiceprintResp>> list() {
        return ApiResponse.success(voiceprintService.listByUser(StpUtil.getLoginIdAsInt()).stream()
            .map(voiceprintConvert::toResp)
            .toList());
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:voiceprint:api:create")
    @AuditLog(module = "声纹识别", operation = "注册声纹")
    @Operation(summary = "注册声纹", description = "上传一段 10 秒左右的单人录音")
    public ApiResponse<VoiceprintResp> register(@RequestParam("memberName") String memberName,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        if (memberName == null || memberName.isBlank()) {
            throw new OperationFailedException("成员称呼不能为空");
        }
        requireAvailable();
        float[] samples = toSamples(file);
        if (samples.length < MIN_SAMPLES) {
            throw new OperationFailedException("录音太短，至少要 1 秒单人说话");
        }
        VoiceprintBO bo = new VoiceprintBO();
        bo.setUserId(StpUtil.getLoginIdAsInt());
        bo.setMemberName(memberName.trim());
        bo.setEmbedding(speakerEmbeddingProvider.embed(samples, 16000));
        bo.setSampleSeconds(BigDecimal.valueOf(samples.length / 16000d).setScale(2, java.math.RoundingMode.HALF_UP));
        return ApiResponse.success(voiceprintConvert.toResp(voiceprintService.register(bo)));
    }

    @DeleteMapping("/{voiceprintId}")
    @ResponseBody
    @SaCheckPermission("system:voiceprint:api:delete")
    @AuditLog(module = "声纹识别", operation = "删除声纹")
    @Operation(summary = "删除声纹", description = "只能删除自己的声纹")
    public ApiResponse<Void> delete(@PathVariable Long voiceprintId) {
        voiceprintService.delete(voiceprintId, StpUtil.getLoginIdAsInt());
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/match")
    @ResponseBody
    @SaCheckPermission("system:voiceprint:api:match")
    @Operation(summary = "识别测试", description = "上传一段录音，看能否认出已注册的成员")
    public ApiResponse<VoiceprintMatchResp> match(@RequestParam("file") MultipartFile file) throws IOException {
        requireAvailable();
        float[] samples = toSamples(file);
        List<VoiceprintBO> candidates = voiceprintService.listEnabled(StpUtil.getLoginIdAsInt());
        VoiceprintMatchResp resp = new VoiceprintMatchResp();
        var result = VoiceprintMatcher.best(speakerEmbeddingProvider.embed(samples, 16000), candidates, threshold);
        if (result.isEmpty()) {
            resp.setMatched(false);
            return ApiResponse.success(resp);
        }
        VoiceprintMatcher.Match match = result.get();
        resp.setVoiceprintId(match.voiceprint().getVoiceprintId());
        resp.setMemberName(match.voiceprint().getMemberName());
        resp.setScore(Math.round(match.score() * 1000d) / 1000d);
        resp.setMatched(match.matched());
        return ApiResponse.success(resp);
    }

    private void requireAvailable() {
        if (!speakerEmbeddingProvider.isAvailable()) {
            throw new OperationFailedException(
                "声纹模型未就绪，请把说话人识别模型放到 xiaozhi.voiceprint.model-path 指定的路径后重启");
        }
    }

    /**
     * 上传音频转 16k 单声道 PCM 浮点样本。设备录音本身就是 16k，网页上传的 wav 统一按同样口径处理。
     */
    private float[] toSamples(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new OperationFailedException("音频文件为空");
        }
        byte[] pcm = AudioUtils.readAsPcm(file.getBytes(), file.getOriginalFilename());
        return AudioUtils.pcm16ToFloats(pcm);
    }
}
