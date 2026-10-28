package com.xiaozhi.voiceclone;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.ai.tts.clone.VoiceCloneClient;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.bo.VoiceCloneBO;
import com.xiaozhi.common.model.resp.VoiceCloneResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import com.xiaozhi.storage.service.StorageServiceFactory;
import com.xiaozhi.voiceclone.convert.VoiceCloneConvert;
import com.xiaozhi.voiceclone.service.VoiceCloneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

/**
 * 音色克隆：上传样本音频 → 调服务商注册 → 记录音色ID，供角色配置选用。
 */
@Slf4j
@RestController
@RequestMapping("/api/voice-clone")
@Tag(name = "音色克隆", description = "把样本音频注册成可用音色")
public class VoiceCloneController extends BaseController {

    @Resource
    private VoiceCloneService voiceCloneService;

    @Resource
    private VoiceCloneConvert voiceCloneConvert;

    @Resource
    private VoiceCloneClient voiceCloneClient;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:voice-clone:api:list")
    @Operation(summary = "克隆列表", description = "当前账号下的克隆记录")
    public ApiResponse<List<VoiceCloneResp>> list() {
        return ApiResponse.success(voiceCloneService.listByUser(StpUtil.getLoginIdAsInt()).stream()
            .map(voiceCloneConvert::toResp)
            .toList());
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:voice-clone:api:create")
    @AuditLog(module = "音色克隆", operation = "创建克隆")
    @Operation(summary = "创建克隆", description = "上传样本音频，异步注册到服务商")
    public ApiResponse<VoiceCloneResp> create(@RequestParam("voiceName") String voiceName,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        if (voiceName == null || voiceName.isBlank()) {
            throw new OperationFailedException("音色名称不能为空");
        }
        if (!voiceCloneClient.available()) {
            throw new OperationFailedException(
                "未接入音色克隆服务商，请先配置阿里云/火山克隆凭据；已有的克隆音色可直接在角色里填写音色ID");
        }
        String relativePath = "voice-clone";
        String uniqueName = System.nanoTime() + "-" + (file.getOriginalFilename() == null ? "sample.wav" : file.getOriginalFilename());
        String storedPath = storageServiceFactory.getStorageService().upload(file, relativePath, uniqueName);

        VoiceCloneBO bo = new VoiceCloneBO();
        bo.setUserId(StpUtil.getLoginIdAsInt());
        bo.setVoiceName(voiceName.trim());
        bo.setProvider(voiceCloneClient.provider());
        bo.setSamplePath(storedPath);
        VoiceCloneBO created = voiceCloneService.create(bo);

        try {
            String audioUrl = storageServiceFactory.accessUrlOf(storedPath);
            String voiceId = voiceCloneClient.enroll(created.getVoiceName(), audioUrl);
            voiceCloneService.markResult(created.getCloneId(), voiceId, null);
        } catch (RuntimeException e) {
            log.warn("音色克隆失败 cloneId={}", created.getCloneId(), e);
            voiceCloneService.markResult(created.getCloneId(), null, e.getMessage());
        }
        return ApiResponse.success(voiceCloneService.listByUser(StpUtil.getLoginIdAsInt()).stream()
            .filter(item -> created.getCloneId().equals(item.getCloneId()))
            .findFirst()
            .map(voiceCloneConvert::toResp)
            .orElseGet(() -> voiceCloneConvert.toResp(created)));
    }

    @DeleteMapping("/{cloneId}")
    @ResponseBody
    @SaCheckPermission("system:voice-clone:api:delete")
    @AuditLog(module = "音色克隆", operation = "删除克隆")
    @Operation(summary = "删除克隆", description = "只删本地记录，服务商侧音色需自行清理")
    public ApiResponse<Void> delete(@PathVariable Long cloneId) {
        voiceCloneService.delete(cloneId, StpUtil.getLoginIdAsInt());
        return ApiResponse.success("删除成功");
    }
}
