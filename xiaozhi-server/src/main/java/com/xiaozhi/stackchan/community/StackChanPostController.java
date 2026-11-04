package com.xiaozhi.stackchan.community;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.StackChanPostBO;
import com.xiaozhi.common.model.bo.StackChanPostCommentBO;
import com.xiaozhi.common.model.req.StackChanPostCommentCreateReq;
import com.xiaozhi.common.model.req.StackChanPostCommentDeleteReq;
import com.xiaozhi.common.model.req.StackChanPostCreateReq;
import com.xiaozhi.common.model.req.StackChanPostDeleteReq;
import com.xiaozhi.common.model.resp.StackChanIdResp;
import com.xiaozhi.common.model.resp.StackChanPostCommentPageResp;
import com.xiaozhi.common.model.resp.StackChanPostCommentResp;
import com.xiaozhi.common.model.resp.StackChanPostResp;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.service.StackChanPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StackChan 固件/App 社区接口：帖子与帖子评论（对应 Go 版后端 {@code api/post/v1}）。
 * <p>
 * 设备身份一律由 {@link StackChanDeviceAuth} 的 Device-Id + HMAC token 确定，
 * 请求体里的 mac 不被信任；帖子时间线是公共的（不按当前设备过滤），但删除只允许发布设备本人操作。
 * <p>
 * 响应保持 Go 版后端的 {@code {"code":0,"message":"","data":...}}，字段名与 Go 版 JSON 对齐
 * （{@code content_text}/{@code content_image} 入参、{@code createdAt} 出参），固件/App 不需要改解析逻辑。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 社区接口", description = "固件/App 侧帖子与评论，设备身份由 Device-Id + HMAC token 确定")
public class StackChanPostController {

    /** Go 版后端按 page<=0 回落到 1、pageSize<=0 回落到 10；这里额外封顶防止超大分页拖垮 DB */
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private StackChanPostService stackChanPostService;

    @Resource
    private DeviceService deviceService;

    @SaIgnore
    @PostMapping("/post/add")
    @ResponseBody
    @Operation(summary = "发布帖子", description = "内容按 Go 版 JSON 键 content_text/content_image 接收；mac 以鉴权结果为准")
    public StackChanApiResponse<StackChanIdResp> addPost(@Valid @RequestBody StackChanPostCreateReq req,
                                                         HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        requireDevice(mac);
        StackChanPostBO post = stackChanPostService.create(mac, req.getContentText(), req.getContentImage());
        return StackChanApiResponse.success(toIdResp(post.getId()));
    }

    @SaIgnore
    @GetMapping("/post/get")
    @ResponseBody
    @Operation(summary = "帖子时间线", description = "按创建时间倒序分页，每条带设备名与评论列表")
    public StackChanApiResponse<List<StackChanPostResp>> getPosts(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
        HttpServletRequest request) {
        stackChanDeviceAuth.authenticate(request);
        List<StackChanPostBO> posts = stackChanPostService.listPage(normalizePage(page), normalizePageSize(pageSize));
        Map<String, String> deviceNames = loadDeviceNames(collectMacs(posts));
        List<StackChanPostResp> result = new ArrayList<>(posts.size());
        for (StackChanPostBO post : posts) {
            result.add(toResp(post, deviceNames));
        }
        return StackChanApiResponse.success(result);
    }

    @SaIgnore
    @DeleteMapping("/post/delete")
    @ResponseBody
    @Operation(summary = "删除帖子", description = "只允许删本设备发布的帖子，评论一并清理")
    public StackChanApiResponse<String> deletePost(@Valid @RequestBody StackChanPostDeleteReq req,
                                                   HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        stackChanPostService.delete(req.getId(), mac);
        return StackChanApiResponse.success("Deletion successful");
    }

    @SaIgnore
    @PostMapping("/post/comment/create")
    @ResponseBody
    @Operation(summary = "发表评论", description = "帖子必须存在；评论设备以鉴权结果为准")
    public StackChanApiResponse<StackChanIdResp> addComment(
        @Valid @RequestBody StackChanPostCommentCreateReq req, HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        StackChanPostCommentBO comment = stackChanPostService.createComment(req.getPostId(), mac, req.getContent());
        return StackChanApiResponse.success(toIdResp(comment.getId()));
    }

    @SaIgnore
    @GetMapping("/post/comment/get")
    @ResponseBody
    @Operation(summary = "帖子评论分页", description = "返回 {list,total}，评论按创建时间正序")
    public StackChanApiResponse<StackChanPostCommentPageResp> getComments(
        @RequestParam(value = "postId", required = false) Long postId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
        HttpServletRequest request) {
        stackChanDeviceAuth.authenticate(request);
        List<StackChanPostCommentBO> comments = stackChanPostService.listComments(
            postId, normalizePage(page), normalizePageSize(pageSize));
        Map<String, String> deviceNames = loadDeviceNames(
            comments.stream().map(StackChanPostCommentBO::getMac).toList());
        List<StackChanPostCommentResp> list = new ArrayList<>(comments.size());
        for (StackChanPostCommentBO comment : comments) {
            list.add(toCommentResp(comment, deviceNames));
        }
        StackChanPostCommentPageResp data = new StackChanPostCommentPageResp();
        data.setList(list);
        data.setTotal(stackChanPostService.countComments(postId));
        return StackChanApiResponse.success(data);
    }

    @SaIgnore
    @DeleteMapping("/post/comment/delete")
    @ResponseBody
    @Operation(summary = "删除评论", description = "只允许删本设备发布的评论，且评论必须属于该帖子")
    public StackChanApiResponse<Void> deleteComment(@Valid @RequestBody StackChanPostCommentDeleteReq req,
                                                    HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        stackChanPostService.deleteComment(req.getPostId(), req.getCommentId(), mac);
        return StackChanApiResponse.success(null);
    }

    /** Go 版后端在发帖前会校验设备档案存在 */
    private void requireDevice(String mac) {
        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            throw new IllegalArgumentException("The device does not exist or the Mac address is incorrect");
        }
    }

    /** 帖子和评论的设备名都取 sys_device.deviceName，一次批量查完，避免逐条打库 */
    private Map<String, String> loadDeviceNames(Collection<String> macs) {
        List<String> distinctMacs = macs.stream()
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        if (distinctMacs.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> names = new HashMap<>();
        for (DeviceBO device : deviceService.listByDeviceIds(distinctMacs)) {
            names.put(device.getDeviceId(),
                StringUtils.hasText(device.getDeviceName()) ? device.getDeviceName() : "");
        }
        return names;
    }

    private List<String> collectMacs(List<StackChanPostBO> posts) {
        List<String> macs = new ArrayList<>();
        for (StackChanPostBO post : posts) {
            macs.add(post.getMac());
            if (post.getPostCommentList() != null) {
                for (StackChanPostCommentBO comment : post.getPostCommentList()) {
                    macs.add(comment.getMac());
                }
            }
        }
        return macs;
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private StackChanIdResp toIdResp(Long id) {
        StackChanIdResp data = new StackChanIdResp();
        data.setId(id);
        return data;
    }

    private StackChanPostResp toResp(StackChanPostBO bo, Map<String, String> deviceNames) {
        StackChanPostResp resp = new StackChanPostResp();
        resp.setId(bo.getId());
        resp.setMac(bo.getMac());
        resp.setName(deviceNames.getOrDefault(bo.getMac(), ""));
        resp.setContentText(bo.getContentText());
        resp.setContentImage(bo.getContentImage());
        resp.setCreatedTime(bo.getCreateTime());
        List<StackChanPostCommentResp> comments = new ArrayList<>();
        if (bo.getPostCommentList() != null) {
            for (StackChanPostCommentBO comment : bo.getPostCommentList()) {
                comments.add(toCommentResp(comment, deviceNames));
            }
        }
        resp.setPostCommentList(comments);
        return resp;
    }

    private StackChanPostCommentResp toCommentResp(StackChanPostCommentBO bo, Map<String, String> deviceNames) {
        StackChanPostCommentResp resp = new StackChanPostCommentResp();
        resp.setId(bo.getId());
        resp.setPostId(bo.getPostId());
        resp.setMac(bo.getMac());
        resp.setName(deviceNames.getOrDefault(bo.getMac(), ""));
        resp.setContent(bo.getContent());
        resp.setCreatedTime(bo.getCreateTime());
        return resp;
    }
}
