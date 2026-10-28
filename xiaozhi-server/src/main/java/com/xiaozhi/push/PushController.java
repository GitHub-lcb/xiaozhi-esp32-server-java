package com.xiaozhi.push;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.req.PushMessageCreateReq;
import com.xiaozhi.common.model.req.PushMessagePageReq;
import com.xiaozhi.common.model.resp.PushMessageResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主动推送：新建一条通知，发给在线设备让它们开口播报。
 */
@RestController
@RequestMapping("/api/push")
@Tag(name = "主动推送", description = "服务端主动向设备播报通知")
public class PushController extends BaseController {

    @Resource
    private PushAppService pushAppService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:push:api:list")
    @Operation(summary = "查询推送列表", description = "按标题与状态分页查询")
    public ApiResponse<PageResult<PushMessageResp>> list(@Valid PushMessagePageReq req) {
        return ApiResponse.success(pushAppService.page(req));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:push:api:create")
    @AuditLog(module = "主动推送", operation = "创建推送")
    @Operation(summary = "新建推送", description = "新建的推送是待发送状态，需要再点发送")
    public ApiResponse<PushMessageResp> create(@Valid @RequestBody PushMessageCreateReq req) {
        return ApiResponse.success(pushAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/{pushId}/send")
    @ResponseBody
    @SaCheckPermission("system:push:api:send")
    @AuditLog(module = "主动推送", operation = "发送推送")
    @Operation(summary = "发送推送", description = "投递给在线设备，离线设备跳过并计数")
    public ApiResponse<PushMessageResp> send(@PathVariable Long pushId) {
        return ApiResponse.success(pushAppService.send(pushId));
    }

    @DeleteMapping("/{pushId}")
    @ResponseBody
    @SaCheckPermission("system:push:api:delete")
    @AuditLog(module = "主动推送", operation = "删除推送")
    @Operation(summary = "删除推送", description = "只有待发送的推送可以删除")
    public ApiResponse<Void> delete(@PathVariable Long pushId) {
        pushAppService.delete(pushId);
        return ApiResponse.success("删除成功");
    }
}
