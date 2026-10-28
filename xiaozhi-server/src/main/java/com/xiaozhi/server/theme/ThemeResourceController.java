package com.xiaozhi.server.theme;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 设备主题资源：唤醒词、表情、配色契约。 */
@RestController
@RequestMapping("/api/theme")
@Tag(name = "主题资源", description = "设备主题资源生成")
public class ThemeResourceController extends BaseController {

    @Resource
    private ThemeResourceService themeResourceService;

    @GetMapping("/{roleId}/resource")
    @ResponseBody
    @SaCheckPermission("system:role:api:list")
    @Operation(summary = "生成主题资源", description = "返回设备可直接读取的 JSON 契约")
    public ApiResponse<Object> resource(@PathVariable Integer roleId) {
        return ApiResponse.success(themeResourceService.build(roleId));
    }
}