package com.xiaozhi.firmware;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareUpgradeLogBO;
import com.xiaozhi.common.model.req.FirmwareUpgradeLogPageReq;
import com.xiaozhi.common.model.resp.FirmwareUpgradeLogResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.firmware.convert.FirmwareUpgradeLogConvert;
import com.xiaozhi.firmware.service.FirmwareUpgradeLogService;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 固件升级记录：谁在什么时候被下发了哪个版本。
 */
@RestController
@RequestMapping("/api/firmware/upgrade-log")
@Tag(name = "固件升级记录", description = "设备固件升级下发记录")
public class FirmwareUpgradeLogController extends BaseController {

    @Resource
    private FirmwareUpgradeLogService upgradeLogService;

    @Resource
    private FirmwareUpgradeLogConvert upgradeLogConvert;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:firmware:upgrade-log:api:list")
    @Operation(summary = "查询升级记录", description = "按设备ID、任务、状态分页查询下发记录")
    public ApiResponse<PageResult<FirmwareUpgradeLogResp>> list(@Valid FirmwareUpgradeLogPageReq req) {
        PageResult<FirmwareUpgradeLogBO> page = upgradeLogService
            .page(req.getPageNo(), req.getPageSize(), req.getDeviceId(), req.getReleaseId(), req.getStatus());
        return ApiResponse.success(page.map(upgradeLogConvert::toResp));
    }
}
