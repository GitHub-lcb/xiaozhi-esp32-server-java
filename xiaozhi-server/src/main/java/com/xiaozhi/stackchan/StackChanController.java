package com.xiaozhi.stackchan;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.model.resp.StackChanAppResp;
import com.xiaozhi.stackchan.model.resp.StackChanDeviceInfoResp;
import com.xiaozhi.stackchan.service.StackChanAppStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * M5Stack StackChan 开源固件兼容接口。
 * <p>
 * 固件把 {@code CONFIG_STACKCHAN_SERVER_URL} 作为前缀再拼 {@code /stackChan/...}，
 * 因此本地部署时把前缀配成 {@code https://<domain>/api} 即可命中本控制器。
 * 响应结构保持 Go 版后端的 {@code {"code":0,...}}，不改固件解析逻辑。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 设备兼容接口", description = "M5Stack StackChan 开源固件的设备侧接口")
public class StackChanController {

    /** 与 App 侧 StackChanDeviceResp.bindTime 保持同一时间格式 */
    private static final DateTimeFormatter BIND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private StackChanDeviceAppService stackChanDeviceAppService;

    @Resource
    private StackChanAppStoreService stackChanAppStoreService;

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private DeviceService deviceService;

    @SaIgnore
    @GetMapping("/device/user")
    @ResponseBody
    @Operation(summary = "查询设备绑定账号", description = "固件 Setup 页展示当前绑定用户名")
    public StackChanApiResponse<String> deviceUser(HttpServletRequest request) {
        return StackChanApiResponse.success(stackChanDeviceAppService.getDeviceUsername(request));
    }

    @SaIgnore
    @GetMapping("/device/info")
    @ResponseBody
    @Operation(summary = "查询设备信息",
        description = "返回 Go model.DeviceInfo 的完整字段（mac/name/uid/bind_time）；固件 Setup 页只读取其中的设备名")
    public StackChanApiResponse<StackChanDeviceInfoResp> deviceInfo(HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        StackChanDeviceInfoResp resp = new StackChanDeviceInfoResp();
        resp.setMac(device.getDeviceId());
        resp.setName(device.getDeviceName());
        resp.setUid(device.getUserId());
        resp.setBindTime(formatBindTime(device.getCreateTime()));
        return StackChanApiResponse.success(resp);
    }

    /** 库里 createTime 是 LocalDateTime，Go 版 bind_time 是 "yyyy-MM-dd HH:mm:ss" 字符串，这里对齐格式 */
    private String formatBindTime(LocalDateTime createTime) {
        return createTime == null ? "" : BIND_TIME_FORMATTER.format(createTime);
    }

    @SaIgnore
    @PostMapping("/device/unbind")
    @ResponseBody
    @Operation(summary = "设备侧解绑", description = "删除设备档案，下次 OTA 重新自动绑定")
    public StackChanApiResponse<Boolean> unbind(HttpServletRequest request) {
        return StackChanApiResponse.success(stackChanDeviceAppService.unbind(request));
    }

    @SaIgnore
    @GetMapping("/apps")
    @Operation(summary = "App Center 列表", description = "下发 App Store 中已上架且未删除的应用，供固件拉取")
    public StackChanApiResponse<List<StackChanAppResp>> apps() {
        List<StackChanAppResp> apps = stackChanAppStoreService.listPublicApps().stream()
            .map(bo -> {
                StackChanAppResp resp = new StackChanAppResp();
                resp.setId(bo.getAppId());
                resp.setAppName(bo.getAppName());
                resp.setAppIconUrl(bo.getAppIconUrl());
                resp.setDescription(bo.getDescription());
                resp.setFirmwareUrl(bo.getFirmwareUrl());
                return resp;
            })
            .toList();
        return StackChanApiResponse.success(apps);
    }
}
