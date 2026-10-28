package com.xiaozhi.ai.plugin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 插件列表与启停。 */
@RestController
@RequestMapping("/api/plugin")
@Tag(name = "插件", description = "已加载插件的查看与启停")
public class PluginController extends BaseController {

    @Resource
    private PluginRegistry pluginRegistry;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:config:api:list")
    @Operation(summary = "插件列表", description = "列出 classpath 上发现的插件")
    public ApiResponse<Object> list() {
        return ApiResponse.success(pluginRegistry.list());
    }

    @PostMapping("/{pluginId}/toggle")
    @ResponseBody
    @SaCheckPermission("system:config:api:update")
    @AuditLog(module = "插件", operation = "启停插件")
    @Operation(summary = "启停插件", description = "状态存内存，重启后回到全部启用")
    public ApiResponse<Void> toggle(@PathVariable String pluginId, @RequestParam boolean enabled) {
        if (!pluginRegistry.setEnabled(pluginId, enabled)) {
            return ApiResponse.error("插件不存在：" + pluginId);
        }
        return ApiResponse.success(enabled ? "已启用" : "已停用");
    }
}