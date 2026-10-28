package com.xiaozhi.firmware;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.req.FirmwareCreateReq;
import com.xiaozhi.common.model.req.FirmwarePageReq;
import com.xiaozhi.common.model.req.FirmwareUpdateReq;
import com.xiaozhi.common.model.resp.FirmwareResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.firmware.convert.FirmwareConvert;
import com.xiaozhi.firmware.service.FirmwareService;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 固件版本管理：OTA 能升级到的版本从这里维护。
 */
@RestController
@RequestMapping("/api/firmware")
@Tag(name = "固件版本管理", description = "设备固件版本相关操作")
public class FirmwareController extends BaseController {

    @Resource
    private FirmwareService firmwareService;

    @Resource
    private FirmwareConvert firmwareConvert;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:firmware:api:list")
    @Operation(summary = "查询固件版本列表", description = "按版本号、芯片型号、状态分页查询")
    public ApiResponse<PageResult<FirmwareResp>> list(@Valid FirmwarePageReq req) {
        return ApiResponse.success(firmwareService
            .page(req.getPageNo(), req.getPageSize(), req.getVersion(), req.getChipModel(), req.getState())
            .map(firmwareConvert::toResp));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:firmware:api:create")
    @AuditLog(module = "固件管理", operation = "创建固件版本")
    @Operation(summary = "新增固件版本", description = "登记一个可下发的固件版本")
    public ApiResponse<FirmwareResp> create(@Valid @RequestBody FirmwareCreateReq req) {
        return ApiResponse.success(firmwareConvert.toResp(firmwareService.create(firmwareConvert.toBO(req))));
    }

    @PutMapping("/{firmwareId}")
    @ResponseBody
    @SaCheckPermission("system:firmware:api:update")
    @AuditLog(module = "固件管理", operation = "更新固件版本")
    @Operation(summary = "更新固件版本", description = "修改版本号、文件地址、发布说明或启停状态")
    public ApiResponse<FirmwareResp> update(@PathVariable Integer firmwareId,
                                            @Valid @RequestBody FirmwareUpdateReq req) {
        return ApiResponse.success(
            firmwareConvert.toResp(firmwareService.update(firmwareId, firmwareConvert.toBO(req))));
    }

    @DeleteMapping("/{firmwareId}")
    @ResponseBody
    @SaCheckPermission("system:firmware:api:delete")
    @AuditLog(module = "固件管理", operation = "删除固件版本")
    @Operation(summary = "删除固件版本", description = "已被升级任务引用的固件不允许删除")
    public ApiResponse<Void> delete(@PathVariable Integer firmwareId) {
        firmwareService.delete(firmwareId);
        return ApiResponse.success("删除成功");
    }
}
