package com.xiaozhi.device;

import com.xiaozhi.server.web.BaseController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.DeviceBatchUpdateReq;
import com.xiaozhi.common.model.req.DeviceCreateReq;
import com.xiaozhi.common.model.req.DevicePageReq;
import com.xiaozhi.common.model.req.DeviceScanBindReq;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.common.model.req.OtaReq;
import com.xiaozhi.common.model.bo.DeviceSelfCheckBO;
import com.xiaozhi.common.model.bo.SelfCheckItemBO;
import com.xiaozhi.common.model.resp.DeviceBatchUpdateResp;
import com.xiaozhi.common.model.resp.DeviceResp;
import com.xiaozhi.common.model.resp.DeviceSelfCheckResp;
import com.xiaozhi.common.model.resp.SelfCheckItemResp;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.common.web.TrustedProxyPolicy;
import com.xiaozhi.utils.JsonUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 设备管理
 * 
 * @author Joey
 * 
 */

@Slf4j
@RestController
@RequestMapping("/api/device")
@Tag(name = "设备管理", description = "设备相关操作")
public class DeviceController extends BaseController {

    @Resource
    private DeviceAppService deviceAppService;

    @Resource
    private TrustedProxyPolicy trustedProxyPolicy;

    @Resource
    private DeviceSelfCheckService deviceSelfCheckService;

    @Resource
    private com.xiaozhi.command.DeviceCommandAppService deviceCommandAppService;

    @Resource
    private com.xiaozhi.screenshot.DeviceScreenshotAppService deviceScreenshotAppService;

    /**
     * 设备远程控制：重启、设置音量、或直接调用设备 MCP 工具
     */
    @PostMapping("/{deviceId}/command")
    @ResponseBody
    @SaCheckPermission("system:device:api:command")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "设备管理", operation = "设备远程控制")
    @Operation(summary = "下发设备控制指令", description = "动作会翻译成设备 MCP 工具调用，离线设备直接拒绝")
    public ApiResponse<com.xiaozhi.common.model.resp.DeviceCommandResp> sendCommand(
            @PathVariable String deviceId,
            @Valid @RequestBody com.xiaozhi.common.model.req.DeviceCommandReq req) {
        return ApiResponse.success(
            deviceCommandAppService.send(deviceId, req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * 设备截图：调用设备截图工具，图片地址由 dialogue 执行完回填
     */
    @PostMapping("/{deviceId}/screenshot")
    @ResponseBody
    @SaCheckPermission("system:device:api:screenshot")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "设备管理", operation = "设备截图")
    @Operation(summary = "设备截图", description = "离线设备直接拒绝，回传的图片地址异步写入记录")
    public ApiResponse<com.xiaozhi.common.model.resp.DeviceScreenshotResp> screenshot(@PathVariable String deviceId) {
        return ApiResponse.success(
            deviceScreenshotAppService.capture(deviceId, StpUtil.getLoginIdAsInt()));
    }

    /**
     * 设备截图记录
     */
    @GetMapping("/screenshot-log")
    @ResponseBody
    @SaCheckPermission("system:device:screenshot:api:list")
    @Operation(summary = "查询设备截图记录", description = "按拍摄时间倒序")
    public ApiResponse<PageResult<com.xiaozhi.common.model.resp.DeviceScreenshotResp>> screenshotLog(
            @Valid com.xiaozhi.common.model.req.DeviceScreenshotPageReq req) {
        return ApiResponse.success(deviceScreenshotAppService.page(req));
    }

    /**
     * 设备指令记录
     */
    @GetMapping("/command-log")
    @ResponseBody
    @SaCheckPermission("system:device:command:api:list")
    @Operation(summary = "查询设备指令记录", description = "按下发时间倒序")
    public ApiResponse<PageResult<com.xiaozhi.common.model.resp.DeviceCommandResp>> commandLog(
            @Valid com.xiaozhi.common.model.req.DeviceCommandPageReq req) {
        return ApiResponse.success(deviceCommandAppService.page(req));
    }

    /**
     * 设备接入一键自检：把「接不进来」最常见的几处判断合成一次调用
     */
    @GetMapping("/{deviceId}/self-check")
    @ResponseBody
    @SaCheckPermission("system:device:api:self-check")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "设备管理", operation = "设备接入自检")
    @Operation(summary = "设备接入自检", description = "逐项检查绑定、在线状态、OTA 地址、实时通道、鉴权与固件版本")
    public ApiResponse<DeviceSelfCheckResp> selfCheck(@PathVariable String deviceId) {
        return ApiResponse.success(toSelfCheckResp(deviceSelfCheckService.check(deviceId)));
    }

    private DeviceSelfCheckResp toSelfCheckResp(DeviceSelfCheckBO bo) {
        DeviceSelfCheckResp resp = new DeviceSelfCheckResp();
        resp.setDeviceId(bo.getDeviceId());
        resp.setDeviceName(bo.getDeviceName());
        resp.setSummary(bo.getSummary());
        resp.setPassed(bo.getPassed());
        resp.setWarned(bo.getWarned());
        resp.setFailed(bo.getFailed());
        resp.setCheckTime(bo.getCheckTime());
        resp.setItems(bo.getItems().stream().map(this::toSelfCheckItemResp).toList());
        return resp;
    }

    private SelfCheckItemResp toSelfCheckItemResp(SelfCheckItemBO bo) {
        SelfCheckItemResp resp = new SelfCheckItemResp();
        resp.setKey(bo.getKey());
        resp.setName(bo.getName());
        resp.setStatus(bo.getStatus());
        resp.setDetail(bo.getDetail());
        resp.setSuggestion(bo.getSuggestion());
        return resp;
    }

    /**
     * 设备查询
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:list")
    @Operation(summary = "根据条件查询设备", description = "返回设备信息列表")
    public ApiResponse<PageResult<DeviceResp>> list(@Valid DevicePageReq req) {
        return ApiResponse.success(deviceAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * 批量更新设备
     */
    @PostMapping("/batchUpdate")
    @ResponseBody
    @SaCheckPermission("system:device:api:batch-update")
    @AuditLog(module = "设备管理", operation = "批量更新设备")
    @CheckOwner(resource = "device", id = "#param.deviceIds != null ? #param.deviceIds.split(',') : null")
    @CheckOwner(resource = "role", id = "#param.roleId")
    @Operation(summary = "批量更新设备", description = "批量更新多个设备的角色")
    public ApiResponse<DeviceBatchUpdateResp> batchUpdate(@Valid @RequestBody DeviceBatchUpdateReq param) {
        DeviceBatchUpdateResp data = deviceAppService.batchUpdate(param);
        return ApiResponse.success("成功更新" + data.getSuccessCount() + "个设备", data);
    }

    /**
     * 添加设备
     */
    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:create")
    @AuditLog(module = "设备管理", operation = "创建设备")
    @Operation(summary = "添加设备", description = "使用设备验证码添加设备到当前用户账户")
    public ApiResponse<DeviceResp> create(@Valid @RequestBody DeviceCreateReq param) {
        return ApiResponse.success(deviceAppService.create(param, StpUtil.getLoginIdAsInt()));
    }

    /**
     * 扫码绑定设备
     */
    @PostMapping("/scan-bind")
    @ResponseBody
    @SaCheckPermission("system:device:api:create")
    @AuditLog(module = "设备管理", operation = "扫码绑定设备")
    @Operation(summary = "扫码绑定设备", description = "使用设备二维码中的设备ID（MAC地址）绑定设备到当前用户账户，要求设备近期在线")
    public ApiResponse<DeviceResp> scanBind(@Valid @RequestBody DeviceScanBindReq param) {
        return ApiResponse.success(deviceAppService.scanBind(param, StpUtil.getLoginIdAsInt()));
    }

    /**
     * 设备信息更新
     */
    @PutMapping("/{deviceId}")
    @ResponseBody
    @SaCheckPermission("system:device:api:update")
    @CheckOwner(resource = "device", id = "#deviceId")
    @CheckOwner(resource = "role", id = "#param.roleId")
    @AuditLog(module = "设备管理", operation = "更新设备")
    @Operation(summary = "更新设备信息", description = "更新设备名称、角色、功能列表等信息")
    public ApiResponse<DeviceResp> update(@PathVariable String deviceId, @Valid @RequestBody DeviceUpdateReq param) {
        return ApiResponse.success(deviceAppService.update(deviceId, param));
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{deviceId}")
    @ResponseBody
    @SaCheckPermission("system:device:api:delete")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "设备管理", operation = "删除设备")
    @Operation(summary = "删除设备", description = "从当前用户账户中删除指定设备")
    public ApiResponse<Void> delete(@PathVariable String deviceId) {
        deviceAppService.delete(deviceId);
        return ApiResponse.success("删除成功");
    }

    @SaIgnore
    @RequestMapping(value = "/ota", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    @Operation(summary = "处理OTA请求", description = "返回OTA结果")
    public ResponseEntity<byte[]> ota(
        @Parameter(description = "设备ID") @RequestHeader(value = "Device-Id", required = false) String deviceIdHeader,
        @RequestBody(required = false) String requestBody,
        HttpServletRequest request) {
        try {
            OtaReq otaReq = parseOtaRequest(deviceIdHeader, requestBody, request);
            Map<String, Object> otaResponse = deviceAppService.handleOta(otaReq);
            return buildJsonResponse(HttpStatus.OK, otaResponse);
        } catch (IllegalArgumentException e) {
            return buildJsonResponse(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("处理OTA请求失败", e);
            return buildJsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "处理请求失败"));
        }
    }

    @SaIgnore
    @PostMapping("/ota/activate")
    @ResponseBody
    @Operation(summary = "查询OTA激活状态", description = "返回OTA激活状态")
    public ResponseEntity<String> otaActivate(
        @Parameter(name = "Device-Id", description = "设备唯一标识", in = ParameterIn.HEADER)
        @RequestHeader(value = "Device-Id", required = false) String deviceId) {
        try {
            return deviceAppService.checkOtaActivation(deviceId)
                    ? ResponseEntity.ok("success")
                    : ResponseEntity.status(202).build();
        } catch (RuntimeException e) {
            log.error("OTA激活失败", e);
            return ResponseEntity.status(202).build();
        }
    }

    /**
     * 从 HTTP 请求中解析出 OTA 所需的设备信息。
     */
    private OtaReq parseOtaRequest(String deviceIdHeader, String requestBody, HttpServletRequest request) {
        OtaReq req = new OtaReq();
        Map<String, Object> jsonData = Map.of();
        if (StringUtils.isNotBlank(requestBody)) {
            try {
                jsonData = JsonUtil.OBJECT_MAPPER.readValue(requestBody, new TypeReference<>() {});
            } catch (IOException e) {
                log.debug("JSON解析失败: {}", e.getMessage());
            }
        }

        // --- 设备ID：优先 Header，其次 Body ---
        if (StringUtils.isNotBlank(deviceIdHeader)) {
            req.setDeviceId(deviceIdHeader);
        } else {
            Object macAddress = jsonData.get("mac_address");
            if (macAddress instanceof String mac && StringUtils.isNotBlank(mac)) {
                req.setDeviceId(mac);
            } else if (jsonData.get("mac") instanceof String mac) {
                req.setDeviceId(mac);
            }
        }

        // --- 硬件 / 网络信息 ---
        if (jsonData.get("chip_model_name") instanceof String chipModel) {
            req.setChipModelName(chipModel);
        }
        if (jsonData.get("application") instanceof Map<?, ?> application
            && application.get("version") instanceof String version) {
            req.setVersion(version);
        }
        if (jsonData.get("board") instanceof Map<?, ?> board) {
            if (board.get("ssid") instanceof String wifiName) {
                req.setWifiName(wifiName);
            }
            if (board.get("type") instanceof String deviceType) {
                req.setType(deviceType);
            }
        }

        // /ota 匿名开放，落库与归属查询用的 IP 只能取可信来源，不采信代理头原文
        req.setIp(trustedProxyPolicy.resolveClientIp(request));
        return req;
    }

    private ResponseEntity<byte[]> buildJsonResponse(HttpStatus status, Object responseData) {
        try {
            byte[] responseBytes = JsonUtil.OBJECT_MAPPER.writeValueAsBytes(responseData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentLength(responseBytes.length);
            return new ResponseEntity<>(responseBytes, headers, status);
        } catch (JsonProcessingException e) {
            log.error("序列化OTA响应失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
