package com.xiaozhi.music;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.req.MusicPageReq;
import com.xiaozhi.common.model.req.MusicPlayLogPageReq;
import com.xiaozhi.common.model.req.MusicUpdateReq;
import com.xiaozhi.common.model.resp.MusicAudioUrlResp;
import com.xiaozhi.common.model.resp.MusicPlayLogResp;
import com.xiaozhi.common.model.resp.MusicResp;
import com.xiaozhi.common.model.resp.MusicStatsResp;
import com.xiaozhi.common.model.resp.MusicSyncResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.music.convert.MusicConvert;
import com.xiaozhi.music.service.MusicLibraryService;
import com.xiaozhi.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 曲库管理：分页列表、统计、编辑上下架、目录同步、删除与试听地址。
 * <p>
 * 音频流不从这里出：试听只下发受保护目录的时效签名，真正的字节流与 Range 由 Spring 的静态资源映射直出，
 * 否则每次拖动进度条都要走一遍 Controller 与权限拦截。
 * <p>
 * 权限一律 {@code @SaCheckPermission(value = "键", orRole = "admin")}：管理员不依赖 sys_auth_role_permission
 * 里是否已经补上授权行，避免"后台刚上线就整页 403"。
 */
@RestController
@RequestMapping("/api/file")
@Tag(name = "曲库管理", description = "曲目列表、编辑、上下架、目录同步与试听")
@Slf4j
public class MusicController {

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Resource
    private MusicLibraryService musicLibraryService;

    @Resource
    private MusicConvert musicConvert;

    @GetMapping("/music")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:list", orRole = "admin")
    @Operation(summary = "曲库分页列表", description = "按关键字/状态筛选，查询前自动补登记目录里新增的 mp3")
    public ApiResponse<PageResult<MusicResp>> list(@Valid MusicPageReq req) {
        return ApiResponse.success(musicLibraryService
                .page(req.getPageNo(), req.getPageSize(), req.getKeyword(), req.getState(),
                        req.getSortBy(), req.getSortOrder())
                .map(musicConvert::toResp));
    }

    @GetMapping("/music/stats")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:list", orRole = "admin")
    @Operation(summary = "曲库统计", description = "总数/上下架数/累计点播/占用空间/今日点播")
    public ApiResponse<MusicStatsResp> stats() {
        return ApiResponse.success(musicConvert.toResp(musicLibraryService.stats()));
    }

    @GetMapping("/music/plays")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:plays", orRole = "admin")
    @Operation(summary = "点播记录", description = "分页返回设备名与命中歌名，查不到给 null")
    public ApiResponse<PageResult<MusicPlayLogResp>> plays(@Valid MusicPlayLogPageReq req) {
        return ApiResponse.success(musicLibraryService
                .pagePlays(req.getPageNo(), req.getPageSize(), req.getKeyword(), req.getDeviceId())
                .map(musicConvert::toResp));
    }

    @PutMapping("/music/{musicId}")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:update", orRole = "admin")
    @AuditLog(module = "曲库", operation = "编辑曲目")
    @Operation(summary = "编辑曲目", description = "歌名/歌手/别名/上下架，记录不存在返回 404")
    public ApiResponse<MusicResp> update(@PathVariable Long musicId, @Valid @RequestBody MusicUpdateReq req) {
        MusicBO patch = musicConvert.toBO(req);
        return ApiResponse.success(musicConvert.toResp(musicLibraryService.update(musicId, patch)));
    }

    @DeleteMapping("/music/{musicId}")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:delete", orRole = "admin")
    @AuditLog(module = "曲库", operation = "删除曲目")
    @Operation(summary = "删除曲目", description = "删除磁盘文件并把记录下架，点播历史保留")
    public ApiResponse<Void> delete(@PathVariable Long musicId) {
        musicLibraryService.delete(musicId);
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/music/sync")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:sync", orRole = "admin")
    @AuditLog(module = "曲库", operation = "同步曲库目录")
    @Operation(summary = "同步曲库目录", description = "新文件登记，文件消失的记录下架")
    public ApiResponse<MusicSyncResp> sync() {
        return ApiResponse.success(musicConvert.toResp(musicLibraryService.sync()));
    }

    @GetMapping("/music/{musicId}/audio-url")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:preview", orRole = "admin")
    @Operation(summary = "试听地址", description = "返回带时效签名的音频相对地址，字节流由静态资源映射直出")
    public ApiResponse<MusicAudioUrlResp> audioUrl(@PathVariable Long musicId) {
        return ApiResponse.success(new MusicAudioUrlResp(musicLibraryService.audioUrl(musicId)));
    }

    @PostMapping("/music")
    @ResponseBody
    @SaCheckPermission(value = "system:file:api:upload", orRole = "admin")
    @AuditLog(module = "曲库", operation = "上传曲目")
    @Operation(summary = "上传曲目", description = "仅支持 .mp3 与 playlist.txt，落盘后立即登记")
    public ApiResponse<String> uploadMusic(@Parameter(description = "上传的音乐文件") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        StorageService.assertAllowed(file);

        // getOriginalFilename 原样返回客户端给的 Content-Disposition filename，含目录分隔符时会写出音乐目录之外
        String safeName = baseName(file.getOriginalFilename());
        if (!StringUtils.hasText(safeName)) {
            throw new IllegalArgumentException("文件名无效");
        }
        if (!safeName.equals("playlist.txt") && !safeName.endsWith(".mp3")) {
            throw new IllegalArgumentException("仅支持 .mp3 或 playlist.txt");
        }
        try {
            Path musicPath = Path.of(runtimePathConfig.getMusicDir()).toAbsolutePath().normalize();
            Files.createDirectories(musicPath);
            Path target = musicPath.resolve(safeName).normalize();
            if (!target.startsWith(musicPath)) {
                throw new IllegalArgumentException("非法文件名");
            }
            file.transferTo(target);
            musicLibraryService.register(safeName);
            return ApiResponse.success("上传成功", safeName);
        } catch (IOException e) {
            log.error("上传失败", e);
            throw new OperationFailedException("上传失败，请稍后重试", e);
        }
    }

    private static String baseName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        Path fileName = Paths.get(originalFilename.replace('\\', '/')).getFileName();
        return fileName == null ? null : fileName.toString();
    }
}
