package com.xiaozhi.stackchan.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xiaozhi.common.model.bo.StackChanDanceBO;
import com.xiaozhi.common.model.bo.StackChanFriendBO;
import com.xiaozhi.common.model.bo.StackChanPanoBO;
import com.xiaozhi.common.model.bo.StackChanPostBO;
import com.xiaozhi.common.model.bo.StackChanPostCommentBO;
import com.xiaozhi.common.model.req.StackChanDanceCreateReq;
import com.xiaozhi.common.model.req.StackChanDanceDeleteReq;
import com.xiaozhi.common.model.req.StackChanDanceUpdateReq;
import com.xiaozhi.common.model.req.StackChanIdDeleteReq;
import com.xiaozhi.common.model.req.StackChanPanoAdminCreateReq;
import com.xiaozhi.common.model.req.StackChanPostDeleteReq;
import com.xiaozhi.common.model.resp.StackChanDanceResp;
import com.xiaozhi.common.model.resp.StackChanFriendResp;
import com.xiaozhi.common.model.resp.StackChanPanoResp;
import com.xiaozhi.common.model.resp.StackChanPostCommentResp;
import com.xiaozhi.common.model.resp.StackChanPostResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.stackchan.service.StackChanDanceService;
import com.xiaozhi.stackchan.service.StackChanFriendService;
import com.xiaozhi.stackchan.service.StackChanPanoService;
import com.xiaozhi.stackchan.service.StackChanPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StackChan 管理端数据页：把 App/设备产生的跳舞、帖子、全景图、好友关系暴露给后台。
 * <p>
 * 只做管理端登录校验（{@link SaCheckLogin}），不新增权限键；实时音视频/动作仍走伴侣 WebSocket，
 * 不属于本控制器的持久化数据范围。
 */
@RestController
@RequestMapping("/api/stackChan/admin")
@Tag(name = "StackChan 管理端数据", description = "跳舞 / 社区帖子 / 全景 / 好友数据管理")
public class StackChanAdminDataController {

    @Resource
    private StackChanDanceService stackChanDanceService;

    @Resource
    private StackChanPostService stackChanPostService;

    @Resource
    private StackChanPanoService stackChanPanoService;

    @Resource
    private StackChanFriendService stackChanFriendService;

    // ==================== 跳舞 ====================

    @SaCheckLogin
    @GetMapping("/dances")
    @ResponseBody
    @Operation(summary = "舞蹈列表", description = "mac 可选，传了则按设备过滤")
    public ApiResponse<List<StackChanDanceResp>> dances(@RequestParam(value = "mac", required = false) String mac) {
        return ApiResponse.success(stackChanDanceService.listAll(mac).stream().map(this::toDanceResp).toList());
    }

    @SaCheckLogin
    @PostMapping("/dances")
    @ResponseBody
    @Operation(summary = "新增舞蹈", description = "需要 mac、danceName、danceData(JSON 数组)")
    public ApiResponse<StackChanDanceResp> createDance(@Valid @RequestBody StackChanDanceCreateReq req) {
        requireDanceArray(req.getDanceData());
        if (!StringUtils.hasText(req.getMac())) {
            throw new IllegalArgumentException("设备 MAC 不能为空");
        }
        StackChanDanceBO bo = new StackChanDanceBO();
        bo.setMac(req.getMac());
        bo.setDanceName(req.getDanceName().trim());
        bo.setDanceData(req.getDanceData().toString());
        bo.setMusicUrl(StringUtils.hasText(req.getMusicUrl()) ? req.getMusicUrl().trim() : null);
        return ApiResponse.success(toDanceResp(stackChanDanceService.create(bo)));
    }

    @SaCheckLogin
    @PutMapping("/dances")
    @ResponseBody
    @Operation(summary = "更新舞蹈", description = "body 传 id，其他字段 null 表示不修改")
    public ApiResponse<StackChanDanceResp> updateDance(@Valid @RequestBody StackChanDanceUpdateReq req) {
        requireDanceArray(req.getDanceData());
        StackChanDanceBO bo = new StackChanDanceBO();
        if (req.getDanceName() != null) {
            bo.setDanceName(req.getDanceName().trim());
        }
        if (req.getDanceData() != null) {
            bo.setDanceData(req.getDanceData().toString());
        }
        if (req.getMusicUrl() != null) {
            bo.setMusicUrl(req.getMusicUrl().trim());
        }
        return ApiResponse.success(toDanceResp(stackChanDanceService.update(req.getId(), bo)));
    }

    @SaCheckLogin
    @DeleteMapping("/dances")
    @ResponseBody
    @Operation(summary = "删除舞蹈")
    public ApiResponse<Void> deleteDance(@Valid @RequestBody StackChanDanceDeleteReq req) {
        stackChanDanceService.delete(req.getId());
        return ApiResponse.success("删除成功");
    }

    // ==================== 社区帖子 ====================

    @SaCheckLogin
    @GetMapping("/posts")
    @ResponseBody
    @Operation(summary = "社区帖子列表", description = "按时间倒序，含评论")
    public ApiResponse<List<StackChanPostResp>> posts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return ApiResponse.success(stackChanPostService.listPage(safePage, safePageSize).stream()
            .map(this::toPostResp).toList());
    }

    @SaCheckLogin
    @DeleteMapping("/posts")
    @ResponseBody
    @Operation(summary = "删除帖子（级联删除评论）")
    public ApiResponse<Void> deletePost(@Valid @RequestBody StackChanPostDeleteReq req) {
        stackChanPostService.deleteByAdmin(req.getId());
        return ApiResponse.success("删除成功");
    }

    // ==================== 全景图 ====================

    @SaCheckLogin
    @GetMapping("/panos")
    @ResponseBody
    @Operation(summary = "全景图列表")
    public ApiResponse<List<StackChanPanoResp>> panos() {
        return ApiResponse.success(stackChanPanoService.listAll().stream().map(this::toPanoResp).toList());
    }

    @SaCheckLogin
    @PostMapping("/panos")
    @ResponseBody
    @Operation(summary = "新增全景图")
    public ApiResponse<StackChanPanoResp> createPano(@Valid @RequestBody StackChanPanoAdminCreateReq req) {
        return ApiResponse.success(toPanoResp(stackChanPanoService.create(req.getMac(), req.getUrl())));
    }

    @SaCheckLogin
    @DeleteMapping("/panos")
    @ResponseBody
    @Operation(summary = "删除全景图")
    public ApiResponse<Void> deletePano(@Valid @RequestBody StackChanIdDeleteReq req) {
        stackChanPanoService.delete(req.getId());
        return ApiResponse.success("删除成功");
    }

    // ==================== 好友 ====================

    @SaCheckLogin
    @GetMapping("/friends")
    @ResponseBody
    @Operation(summary = "好友关系列表")
    public ApiResponse<List<StackChanFriendResp>> friends() {
        return ApiResponse.success(stackChanFriendService.listAll().stream().map(this::toFriendResp).toList());
    }

    @SaCheckLogin
    @DeleteMapping("/friends")
    @ResponseBody
    @Operation(summary = "删除好友关系")
    public ApiResponse<Void> deleteFriend(@Valid @RequestBody StackChanIdDeleteReq req) {
        stackChanFriendService.delete(req.getId());
        return ApiResponse.success("删除成功");
    }

    private void requireDanceArray(com.fasterxml.jackson.databind.JsonNode danceData) {
        if (danceData != null && !danceData.isArray()) {
            throw new IllegalArgumentException("danceData 必须是 JSON 数组");
        }
    }

    private StackChanDanceResp toDanceResp(StackChanDanceBO bo) {
        StackChanDanceResp resp = new StackChanDanceResp();
        resp.setId(bo.getDanceId());
        resp.setMac(bo.getMac());
        resp.setDanceName(bo.getDanceName());
        resp.setMusicUrl(bo.getMusicUrl());
        resp.setDanceData(bo.getDanceData());
        return resp;
    }

    private StackChanPostResp toPostResp(StackChanPostBO bo) {
        StackChanPostResp resp = new StackChanPostResp();
        resp.setId(bo.getId());
        resp.setMac(bo.getMac());
        resp.setContentText(bo.getContentText());
        resp.setContentImage(bo.getContentImage());
        resp.setCreatedTime(bo.getCreateTime());
        if (bo.getPostCommentList() != null) {
            resp.setPostCommentList(bo.getPostCommentList().stream().map(this::toCommentResp).toList());
        }
        return resp;
    }

    private StackChanPostCommentResp toCommentResp(StackChanPostCommentBO bo) {
        StackChanPostCommentResp resp = new StackChanPostCommentResp();
        resp.setId(bo.getId());
        resp.setPostId(bo.getPostId());
        resp.setMac(bo.getMac());
        resp.setContent(bo.getContent());
        resp.setCreatedTime(bo.getCreateTime());
        return resp;
    }

    private StackChanPanoResp toPanoResp(StackChanPanoBO bo) {
        StackChanPanoResp resp = new StackChanPanoResp();
        resp.setId(bo.getId());
        resp.setMac(bo.getMac());
        resp.setPanoUrl(bo.getPanoUrl());
        resp.setCreatedTime(bo.getCreateTime());
        resp.setUpdatedTime(bo.getUpdateTime());
        return resp;
    }

    private StackChanFriendResp toFriendResp(StackChanFriendBO bo) {
        StackChanFriendResp resp = new StackChanFriendResp();
        resp.setId(bo.getId());
        resp.setMacA(bo.getMacA());
        resp.setMacB(bo.getMacB());
        resp.setCreateTime(bo.getCreateTime());
        return resp;
    }
}
