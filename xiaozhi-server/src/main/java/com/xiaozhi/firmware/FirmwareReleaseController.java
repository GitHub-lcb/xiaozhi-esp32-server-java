package com.xiaozhi.firmware;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.common.model.bo.FirmwareReleaseBO;
import com.xiaozhi.common.model.req.FirmwareReleaseCreateReq;
import com.xiaozhi.common.model.req.FirmwareReleasePageReq;
import com.xiaozhi.common.model.req.FirmwareReleaseStateReq;
import com.xiaozhi.common.model.req.FirmwareReleaseUpdateReq;
import com.xiaozhi.common.model.resp.FirmwareReleaseResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.firmware.convert.FirmwareReleaseConvert;
import com.xiaozhi.firmware.service.FirmwareReleaseService;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 固件升级任务：灰度策略、时间窗与发布/暂停/结束。
 */
@RestController
@RequestMapping("/api/firmware/release")
@Tag(name = "固件升级任务", description = "固件分批升级相关操作")
public class FirmwareReleaseController extends BaseController {

    @Resource
    private FirmwareReleaseService firmwareReleaseService;

    @Resource
    private FirmwareService firmwareService;

    @Resource
    private FirmwareReleaseConvert releaseConvert;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:firmware:release:api:list")
    @Operation(summary = "查询升级任务列表", description = "按名称、目标固件、状态分页查询")
    public ApiResponse<PageResult<FirmwareReleaseResp>> list(@Valid FirmwareReleasePageReq req) {
        PageResult<FirmwareReleaseBO> page = firmwareReleaseService
            .page(req.getPageNo(), req.getPageSize(), req.getReleaseName(), req.getFirmwareId(), req.getState());
        return ApiResponse.success(toRespPage(page));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:firmware:release:api:create")
    @AuditLog(module = "固件管理", operation = "创建升级任务")
    @Operation(summary = "新增升级任务", description = "新建的任务是待开始状态，需要再发布")
    public ApiResponse<FirmwareReleaseResp> create(@Valid @RequestBody FirmwareReleaseCreateReq req) {
        FirmwareReleaseBO created = firmwareReleaseService.create(releaseConvert.toBO(req));
        return ApiResponse.success(toResp(created, versionOf(created.getFirmwareId())));
    }

    @PutMapping("/{releaseId}")
    @ResponseBody
    @SaCheckPermission("system:firmware:release:api:update")
    @AuditLog(module = "固件管理", operation = "更新升级任务")
    @Operation(summary = "更新升级任务", description = "修改目标固件、策略、白名单或时间窗")
    public ApiResponse<FirmwareReleaseResp> update(@PathVariable Integer releaseId,
                                                   @Valid @RequestBody FirmwareReleaseUpdateReq req) {
        FirmwareReleaseBO updated = firmwareReleaseService.update(releaseId, releaseConvert.toBO(req));
        return ApiResponse.success(toResp(updated, versionOf(updated.getFirmwareId())));
    }

    @PostMapping("/{releaseId}/state")
    @ResponseBody
    @SaCheckPermission("system:firmware:release:api:state")
    @AuditLog(module = "固件管理", operation = "变更升级任务状态")
    @Operation(summary = "变更任务状态", description = "1 发布(进行中)、2 暂停、3 结束")
    public ApiResponse<FirmwareReleaseResp> changeState(@PathVariable Integer releaseId,
                                                        @Valid @RequestBody FirmwareReleaseStateReq req) {
        FirmwareReleaseBO changed = firmwareReleaseService.changeState(releaseId, req.getState());
        return ApiResponse.success(toResp(changed, versionOf(changed.getFirmwareId())));
    }

    @DeleteMapping("/{releaseId}")
    @ResponseBody
    @SaCheckPermission("system:firmware:release:api:delete")
    @AuditLog(module = "固件管理", operation = "删除升级任务")
    @Operation(summary = "删除升级任务", description = "进行中的任务不能删除")
    public ApiResponse<Void> delete(@PathVariable Integer releaseId) {
        firmwareReleaseService.delete(releaseId);
        return ApiResponse.success("删除成功");
    }

    private PageResult<FirmwareReleaseResp> toRespPage(PageResult<FirmwareReleaseBO> page) {
        List<Integer> firmwareIds = page.getList().stream()
            .map(FirmwareReleaseBO::getFirmwareId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        Map<Integer, String> versionMap = firmwareIds.isEmpty() ? Map.of()
            : firmwareService.listByIds(firmwareIds).stream()
                .collect(Collectors.toMap(FirmwareBO::getFirmwareId, FirmwareBO::getVersion, (a, b) -> a));
        return page.map(bo -> toResp(bo, versionMap.get(bo.getFirmwareId())));
    }

    private String versionOf(Integer firmwareId) {
        if (firmwareId == null) {
            return null;
        }
        return firmwareService.listByIds(List.of(firmwareId)).stream()
            .findFirst()
            .map(FirmwareBO::getVersion)
            .orElse(null);
    }

    private FirmwareReleaseResp toResp(FirmwareReleaseBO bo, String firmwareVersion) {
        FirmwareReleaseResp resp = releaseConvert.toResp(bo);
        resp.setFirmwareVersion(firmwareVersion);
        return resp;
    }
}
