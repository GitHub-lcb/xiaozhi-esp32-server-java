package com.xiaozhi.stackchan.app;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.stackchan.app.model.req.StackChanDeviceUpdateReq;
import com.xiaozhi.stackchan.app.model.req.StackChanLoginReq;
import com.xiaozhi.stackchan.app.model.req.StackChanMacReq;
import com.xiaozhi.stackchan.app.model.req.StackChanRegistrationReq;
import com.xiaozhi.stackchan.app.model.resp.StackChanDeviceResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanLoginResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanRegistrationResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanUserInfoResp;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StackChan App 兼容接口（对应 Go 版后端 {@code /stackChan/v2/**}）。
 * <p>
 * 改造后的 Flutter App 的 baseUrl 是 {@code https://<host>/api/stackChan/}，
 * 因此这里的类级映射是 {@code /api/stackChan/v2}，与固件侧 {@link com.xiaozhi.stackchan.StackChanController}
 * （{@code /api/stackChan}）同前缀不同路径段，不冲突。
 * <p>
 * 响应结构固定为 Go 版的 {@code {"code":0,"message":"","data":...}}，直接复用
 * {@link StackChanApiResponse}；管理端拦截器由每个方法上的 {@link SaIgnore} 关闭，
 * 登录态在 {@link StackChanAppTokenAuth} 中自行校验（token 头或 Authorization: Bearer）。
 */
@RestController
@RequestMapping("/api/stackChan/v2")
@Tag(name = "StackChan App 兼容接口", description = "M5Stack StackChan App 的用户/设备兼容接口")
public class StackChanAppController {

    @Resource
    private StackChanAppTokenAuth stackChanAppTokenAuth;

    @Resource
    private StackChanAppUserAppService stackChanAppUserAppService;

    @Resource
    private StackChanAppDeviceAppService stackChanAppDeviceAppService;

    // ==================== 用户 ====================

    @SaIgnore
    @PostMapping("/user/login")
    @Operation(summary = "App 登录", description = "用户名/邮箱/手机号 + 密码登录，返回 Sa-Token")
    public StackChanApiResponse<StackChanLoginResp> login(
        @RequestBody(required = false) StackChanLoginReq req, HttpServletRequest request) {
        return StackChanApiResponse.success(stackChanAppUserAppService.login(req, request));
    }

    @SaIgnore
    @PostMapping("/user/registration")
    @Operation(summary = "App 注册", description = "直接建号（App 不传邮箱验证码），返回 NodeBB 风格用户信息")
    public StackChanApiResponse<StackChanRegistrationResp> registration(
        @RequestBody(required = false) StackChanRegistrationReq req) {
        return StackChanApiResponse.success(stackChanAppUserAppService.register(req));
    }

    @SaIgnore
    @GetMapping("/user")
    @Operation(summary = "当前用户信息", description = "按 token 返回当前登录用户资料")
    public StackChanApiResponse<StackChanUserInfoResp> user(HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppUserAppService.getUserInfo(userId));
    }

    // ==================== 设备 ====================

    @SaIgnore
    @GetMapping("/devices")
    @Operation(summary = "当前用户设备列表", description = "mac 返回 12 位小写纯 hex，过滤网页聊天虚拟设备")
    public StackChanApiResponse<List<StackChanDeviceResp>> devices(HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.listDevices(userId));
    }

    @SaIgnore
    @PostMapping("/device/bind")
    @Operation(summary = "绑定设备", description = "已绑当前用户幂等成功；绑定他人报错；未建档则用默认角色建档并绑定")
    public StackChanApiResponse<Boolean> bindDevice(
        @RequestBody(required = false) StackChanMacReq req, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.bindDevice(userId, req));
    }

    @SaIgnore
    @PostMapping("/device/activate_bind")
    @Operation(summary = "激活并绑定设备", description = "自建后端无云激活流程，语义与 /device/bind 相同")
    public StackChanApiResponse<Boolean> activateAndBindDevice(
        @RequestBody(required = false) StackChanMacReq req, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.bindDevice(userId, req));
    }

    @SaIgnore
    @PutMapping("/device/update")
    @Operation(summary = "修改设备名", description = "只允许修改当前用户名下的设备")
    public StackChanApiResponse<Boolean> updateDevice(
        @RequestBody(required = false) StackChanDeviceUpdateReq req, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.updateDevice(userId, req));
    }

    @SaIgnore
    @PostMapping("/device/unbind")
    @Operation(summary = "解绑设备", description = "删除设备档案，设备下次 OTA 重新自动建档")
    public StackChanApiResponse<Boolean> unbindDevice(
        @RequestBody(required = false) StackChanMacReq req, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.unbindDevice(userId, req));
    }

    @SaIgnore
    @PostMapping("/device/agent/restore")
    @Operation(summary = "恢复默认 Agent", description = "P0 占位，直接返回成功")
    public StackChanApiResponse<Boolean> restoreDefaultAgent(HttpServletRequest request) {
        stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAppDeviceAppService.restoreDefaultAgent());
    }
}
