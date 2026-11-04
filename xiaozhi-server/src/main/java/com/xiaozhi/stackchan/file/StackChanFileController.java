package com.xiaozhi.stackchan.file;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.resp.StackChanFileUploadResp;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.store.StackChanAppAuth;
import com.xiaozhi.storage.service.StorageService;
import com.xiaozhi.storage.service.StorageServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * StackChan 文件上传与公开下载。
 * <p>
 * 上传复用 xiaozhi-service 的 {@link StorageServiceFactory}/{@link StorageService}（本地或云存储由后台配置决定），
 * 统一落在 {@code uploads/stackchan/<directory>/} 下；下载通过 {@code /api/stackChan/file/**} 公开只读，
 * 供 App Store 的图标与固件安装包直接引用，因此必须在服务端把路径锁死在 stackchan 上传目录内：
 * 拒绝 {@code ..}、拒绝 .html/.svg 这类能被浏览器当页面渲染的类型。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 文件接口", description = "设备/App 文件上传与公开只读下载")
public class StackChanFileController {

    /** StackChan 在存储根（xiaozhi.upload-path，默认 uploads）下的子目录 */
    private static final String STACKCHAN_STORAGE_DIR = "stackchan";

    /** 公开下载 URL 前缀，必须与 {@link #download(HttpServletRequest)} 的映射一致 */
    private static final String PUBLIC_FILE_PREFIX = "/api/stackChan/file/";

    /** 上传目录白名单：只允许一级目录名，天然挡住路径穿越；moments 是 App 上传音乐文件时用的目录 */
    private static final Set<String> ALLOWED_DIRECTORIES = Set.of(
        "apps", "icons", "firmware", "music", "dance", "posts", "pano", "avatar", "moments", "common");

    /** 上传扩展名白名单：与 FileUploadController 口径一致，去掉 .html/.svg 等可被浏览器渲染的类型 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
        ".mp3", ".wav", ".ogg", ".opus", ".flac", ".aac", ".m4a",
        ".mp4", ".mov", ".webm",
        ".bin", ".hex", ".img", ".uf2", ".zip", ".tar", ".gz", ".7z", ".rar",
        ".json", ".txt", ".pdf");

    /** 下载时直接拒绝的危险类型：内嵌脚本在同源执行或可被当 HTML 解析 */
    private static final Set<String> FORBIDDEN_DOWNLOAD_EXTENSIONS = Set.of(
        ".html", ".htm", ".xhtml", ".shtml", ".svg", ".js", ".mjs", ".php", ".jsp", ".sh");

    private static final int MAX_FILE_NAME_LENGTH = 128;

    /** 与 LocalStorageService 同一配置项，下载时拼回存储键用 */
    @Value("${xiaozhi.upload-path:uploads}")
    private String uploadPath;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private StackChanAppAuth stackChanAppAuth;

    /**
     * 设备与 App 共用的上传接口：带 Device-Id 走设备 HMAC 鉴权，否则按 App 登录态校验。
     * <p>
     * 参数与 Go 版一致：multipart 的 file/name/directory；name 缺省时取原始文件名。
     */
    @SaIgnore
    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @Operation(summary = "上传文件", description = "directory 只允许白名单一级目录；返回 file/... 展示路径与公开下载 URL")
    public StackChanApiResponse<StackChanFileUploadResp> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "directory", required = false) String directory,
            HttpServletRequest request) {

        authenticateUploader(request);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        StorageService.assertAllowed(file);

        String safeDirectory = normalizeDirectory(directory);
        String fileName = resolveFileName(name, file.getOriginalFilename());

        StorageService storageService = storageServiceFactory.getStorageService();
        String storedPath;
        try {
            storedPath = storageService.upload(file, STACKCHAN_STORAGE_DIR + "/" + safeDirectory, fileName);
        } catch (IOException e) {
            throw new OperationFailedException("文件上传失败，请稍后重试", e);
        }

        String publicRelativePath = safeDirectory + "/" + fileName;
        StackChanFileUploadResp resp = new StackChanFileUploadResp();
        // 与 Go 版保持一致的展示路径：file/<directory>/<name>
        resp.setPath("file/" + publicRelativePath);
        if (isRemoteUrl(storedPath)) {
            // 云端存储：直接用当前存储实现给出的可访问地址（私有桶会带签名）
            resp.setUrl(storageService.getAccessUrl(storedPath));
        } else {
            // 路径段做 URL 编码，中文/空格文件名拼出来的地址才是合法的
            String encodedPath = safeDirectory + "/" + UriUtils.encodePathSegment(fileName, StandardCharsets.UTF_8);
            resp.setUrl(serverAddressProvider.getServerAddress() + PUBLIC_FILE_PREFIX + encodedPath);
        }
        return StackChanApiResponse.success(resp);
    }

    /**
     * 公开只读下载：仅允许 stackchan 上传目录下的文件，路径逐段校验，危险扩展名直接拒绝。
     */
    @SaIgnore
    @GetMapping("/file/**")
    @Operation(summary = "公开下载文件", description = "固定映射到 uploads/stackchan/ 下的文件，供 App Store 图标与固件下载")
    public ResponseEntity<ByteArrayResource> download(HttpServletRequest request) {
        String relativePath = extractRelativePath(request);
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        String extension = extensionOf(fileName);
        if (FORBIDDEN_DOWNLOAD_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不允许下载该类型文件");
        }

        String storageKey = uploadPath + "/" + STACKCHAN_STORAGE_DIR + "/" + relativePath;
        byte[] content = storageServiceFactory.downloadFrom(storageKey);
        if (content == null || content.length == 0) {
            throw new ResourceNotFoundException("文件不存在");
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .contentType(mediaType)
            .contentLength(content.length)
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        if (!isInlineMediaType(mediaType)) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName));
        }
        return builder.body(new ByteArrayResource(content));
    }

    private void authenticateUploader(HttpServletRequest request) {
        if (StringUtils.hasText(request.getHeader("Device-Id"))) {
            stackChanDeviceAuth.authenticate(request);
            return;
        }
        stackChanAppAuth.requireLogin(request);
    }

    private String normalizeDirectory(String rawDirectory) {
        if (!StringUtils.hasText(rawDirectory)) {
            return "common";
        }
        String directory = rawDirectory.trim();
        if (!ALLOWED_DIRECTORIES.contains(directory)) {
            throw new IllegalArgumentException("不支持的上传目录: " + rawDirectory);
        }
        return directory;
    }

    private String resolveFileName(String rawName, String originalFilename) {
        String candidate = StringUtils.hasText(rawName) ? rawName : originalFilename;
        if (!StringUtils.hasText(candidate)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        // 只取最后一段，客户端带上目录也不会写到别处
        String fileName = candidate.trim();
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (separator >= 0) {
            fileName = fileName.substring(separator + 1);
        }
        fileName = fileName.replace("\u0000", "").trim();
        if (!StringUtils.hasText(fileName) || ".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        // 以点开头的隐藏文件会被 Sa-Token 防火墙按黑路径拦截，下发 URL 打不开，直接拒绝
        if (fileName.startsWith(".")) {
            throw new IllegalArgumentException("文件名不能以点开头");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("文件名过长，最多 " + MAX_FILE_NAME_LENGTH + " 个字符");
        }
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }
        return fileName;
    }

    /** 从请求 URI 中还原 /** 之后的相对路径，解码后再按段校验，防止 %2e%2e 之类绕过 */
    private String extractRelativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        int prefixIndex = uri.indexOf(PUBLIC_FILE_PREFIX);
        if (prefixIndex < 0) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        String rawPath = uri.substring(prefixIndex + PUBLIC_FILE_PREFIX.length());
        String decodedPath;
        try {
            // 只做百分号解码（不把 + 当空格），编码过的 .. 也要按解码后的形态校验
            decodedPath = UriUtils.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        return normalizeRelativePath(decodedPath);
    }

    private String normalizeRelativePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        String path = rawPath.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!StringUtils.hasText(path) || path.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("文件路径不合法");
            }
            if (segment.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("文件路径不合法");
            }
        }
        return String.join("/", segments);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private boolean isRemoteUrl(String storedPath) {
        if (storedPath == null) {
            return false;
        }
        String lower = storedPath.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private boolean isInlineMediaType(MediaType mediaType) {
        String type = mediaType.getType();
        return "image".equals(type) || "audio".equals(type) || "video".equals(type);
    }

    /** Content-Disposition 里只放 ASCII，非 ASCII 文件名替换成下划线，避免响应头编码问题 */
    private String contentDisposition(String fileName) {
        StringBuilder asciiName = new StringBuilder(fileName.length());
        for (char ch : fileName.toCharArray()) {
            boolean asciiSafe = ch < 128 && (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_');
            asciiName.append(asciiSafe ? ch : '_');
        }
        return "attachment; filename=\"" + asciiName + "\"";
    }
}
