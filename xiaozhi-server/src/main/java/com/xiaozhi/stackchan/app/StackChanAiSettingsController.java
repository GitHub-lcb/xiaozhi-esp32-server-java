package com.xiaozhi.stackchan.app;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.stackchan.app.model.req.StackChanAiConversationRenameReq;
import com.xiaozhi.stackchan.app.model.req.StackChanAiRoleUpdateReq;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiConversationPageResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiMemoryResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiMessagePageResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiOptionsResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiRoleResp;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * StackChan App「AI 设置」接口（{@code /api/stackChan/v2/ai}）。
 * <p>
 * 契约见 {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md}：App 登录后拿到的同一个 Sa-Token
 * 放在 {@code token} 头里，登录态由 {@link StackChanAppTokenAuth} 自行校验（与管理端拦截器无关）。
 * 全部接口都用 {@link SaIgnore} 绕过管理端登录拦截，数据一律限定当前 userId。
 * <p>
 * 响应结构沿用 App v2 的 {@link StackChanApiResponse}（成功码 0），与管理端 {@code ApiResponse}（成功码 200）
 * 刻意区分，App 端只认 code==0。这些接口不需要任何管理端权限（{@code system:role:api:*} 等），
 * 普通 App 用户即可访问自己的角色 / 设备记忆 / 会话。
 */
@RestController
@RequestMapping("/api/stackChan/v2/ai")
@Tag(name = "StackChan App AI 设置", description = "App 的角色、模型音色、记忆与对话记录接口")
public class StackChanAiSettingsController {

    @Resource
    private StackChanAppTokenAuth stackChanAppTokenAuth;

    @Resource
    private StackChanAiSettingsAppService stackChanAiSettingsAppService;

    // ==================== 角色配置 ====================

    @SaIgnore
    @GetMapping("/role")
    @Operation(summary = "读取默认 AI 角色配置", description = "当前用户的默认角色；没有设备时 deviceMac 为 null")
    public StackChanApiResponse<StackChanAiRoleResp> getRole(HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.getRole(userId));
    }

    @SaIgnore
    @PutMapping("/role")
    @Operation(summary = "保存默认 AI 角色配置", description = "只更新传入字段；角色固定取当前用户默认角色，不接受 roleId")
    public StackChanApiResponse<Boolean> updateRole(
        @RequestBody(required = false) StackChanAiRoleUpdateReq req, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.updateRole(userId, req));
    }

    // ==================== 可选模型 / 音色 / 识别引擎 ====================

    @SaIgnore
    @GetMapping("/options")
    @Operation(summary = "可选模型 / 音色 / 识别引擎", description = "来自当前用户自己的 sys_config，不含任何密钥")
    public StackChanApiResponse<StackChanAiOptionsResp> options(HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.options(userId));
    }

    // ==================== 记忆 ====================

    @SaIgnore
    @GetMapping("/memory")
    @Operation(summary = "查询设备记忆", description = "摘要 + 长期记忆事实；deviceMac 不传时取第一台设备")
    public StackChanApiResponse<StackChanAiMemoryResp> memory(
        @RequestParam(required = false) String deviceMac, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.memory(userId, deviceMac));
    }

    @SaIgnore
    @DeleteMapping("/memory")
    @Operation(summary = "清空设备记忆", description = "删除该设备（+该角色）的摘要，返回实际删除条数")
    public StackChanApiResponse<Integer> clearMemory(
        @RequestParam(required = false) String deviceMac, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.clearMemory(userId, deviceMac));
    }

    @SaIgnore
    @DeleteMapping("/memory-fact/{factId}")
    @Operation(summary = "删除单条记忆事实", description = "只能删除当前用户自己的事实")
    public StackChanApiResponse<Boolean> deleteMemoryFact(
        @PathVariable Long factId, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(stackChanAiSettingsAppService.deleteMemoryFact(userId, factId));
    }

    // ==================== 对话记录 ====================

    @SaIgnore
    @GetMapping("/conversations")
    @Operation(summary = "对话记录会话列表", description = "按当前用户过滤，最后消息时间倒序")
    public StackChanApiResponse<StackChanAiConversationPageResp> conversations(
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(
            stackChanAiSettingsAppService.conversations(userId, pageNo, pageSize));
    }

    @SaIgnore
    @GetMapping("/conversations/{sessionId}/messages")
    @Operation(summary = "会话消息列表", description = "会话不属于当前用户时按「会话不存在」报错")
    public StackChanApiResponse<StackChanAiMessagePageResp> messages(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "50") Integer pageSize,
        HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(
            stackChanAiSettingsAppService.messages(userId, sessionId, pageNo, pageSize));
    }

    @SaIgnore
    @PatchMapping("/conversations/{sessionId}")
    @Operation(summary = "重命名会话", description = "只能改当前用户名下的会话")
    public StackChanApiResponse<Boolean> renameConversation(
        @PathVariable String sessionId,
        @RequestBody(required = false) StackChanAiConversationRenameReq req,
        HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(
            stackChanAiSettingsAppService.renameConversation(userId, sessionId, req));
    }

    @SaIgnore
    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "删除会话", description = "连带该会话的消息与摘要一起删除")
    public StackChanApiResponse<Boolean> deleteConversation(
        @PathVariable String sessionId, HttpServletRequest request) {
        Integer userId = stackChanAppTokenAuth.requireUserId(request);
        return StackChanApiResponse.success(
            stackChanAiSettingsAppService.deleteConversation(userId, sessionId));
    }
}
