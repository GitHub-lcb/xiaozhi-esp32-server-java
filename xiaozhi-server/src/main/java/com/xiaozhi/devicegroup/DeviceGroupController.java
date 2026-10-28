package com.xiaozhi.devicegroup;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.devicegroup.service.DeviceGroupService;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备分组：同空间的设备放一组，唤醒时只由一台应答。
 * 权限复用设备管理（list / update），不额外新增权限键。
 */
@RestController
@RequestMapping("/api/device-group")
@Tag(name = "设备分组", description = "同空间设备分组与成员维护")
public class DeviceGroupController extends BaseController {

    @Resource
    private DeviceGroupService deviceGroupService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:list")
    @Operation(summary = "分组列表", description = "返回分组及其成员设备")
    public ApiResponse<Object> list(@RequestParam("groupId") Long groupId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (groupId != null) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("groupId", groupId);
            group.put("deviceIds", deviceGroupService.listDeviceIds(groupId));
            result.add(group);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:update")
    @AuditLog(module = "设备分组", operation = "创建分组")
    @Operation(summary = "创建分组", description = "返回新分组ID")
    public ApiResponse<Long> create(@RequestParam("groupName") String groupName) {
        return ApiResponse.success(deviceGroupService.createGroup(StpUtil.getLoginIdAsInt(), groupName));
    }

    @PostMapping("/{groupId}/member")
    @ResponseBody
    @SaCheckPermission("system:device:api:update")
    @AuditLog(module = "设备分组", operation = "添加成员")
    @Operation(summary = "添加成员", description = "把设备加入分组")
    public ApiResponse<Void> addMember(@PathVariable Long groupId, @RequestParam("deviceId") String deviceId) {
        deviceGroupService.addMember(groupId, deviceId);
        return ApiResponse.success("已加入");
    }

    @DeleteMapping("/{groupId}/member/{deviceId}")
    @ResponseBody
    @SaCheckPermission("system:device:api:update")
    @AuditLog(module = "设备分组", operation = "移除成员")
    @Operation(summary = "移除成员", description = "把设备移出分组")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable String deviceId) {
        deviceGroupService.removeMember(groupId, deviceId);
        return ApiResponse.success("已移除");
    }
}