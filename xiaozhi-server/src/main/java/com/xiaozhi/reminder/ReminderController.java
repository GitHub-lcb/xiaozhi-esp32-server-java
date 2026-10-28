package com.xiaozhi.reminder;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.req.ReminderCreateReq;
import com.xiaozhi.common.model.req.ReminderPageReq;
import com.xiaozhi.common.model.resp.ReminderResp;
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
 * 语音提醒与闹钟。
 */
@RestController
@RequestMapping("/api/reminder")
@Tag(name = "语音提醒", description = "到点让设备播报提醒或闹钟")
public class ReminderController extends BaseController {

    @Resource
    private ReminderAppService reminderAppService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:reminder:api:list")
    @Operation(summary = "查询提醒列表", description = "按设备与状态分页查询")
    public ApiResponse<PageResult<ReminderResp>> list(@Valid ReminderPageReq req) {
        return ApiResponse.success(reminderAppService.page(req));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:reminder:api:create")
    @AuditLog(module = "语音提醒", operation = "创建提醒")
    @Operation(summary = "新建提醒", description = "支持一次性、每天、工作日重复")
    public ApiResponse<ReminderResp> create(@Valid @RequestBody ReminderCreateReq req) {
        return ApiResponse.success(reminderAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/{reminderId}/cancel")
    @ResponseBody
    @SaCheckPermission("system:reminder:api:cancel")
    @AuditLog(module = "语音提醒", operation = "取消提醒")
    @Operation(summary = "取消提醒", description = "只有待播报的提醒可以取消")
    public ApiResponse<ReminderResp> cancel(@PathVariable Long reminderId) {
        return ApiResponse.success(reminderAppService.cancel(reminderId));
    }

    @DeleteMapping("/{reminderId}")
    @ResponseBody
    @SaCheckPermission("system:reminder:api:delete")
    @AuditLog(module = "语音提醒", operation = "删除提醒")
    @Operation(summary = "删除提醒", description = "删除后不再播报")
    public ApiResponse<Void> delete(@PathVariable Long reminderId) {
        reminderAppService.delete(reminderId);
        return ApiResponse.success("删除成功");
    }
}
