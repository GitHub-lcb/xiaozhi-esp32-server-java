package com.xiaozhi.stackchan.community;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.StackChanPanoBO;
import com.xiaozhi.common.model.req.StackChanPanoCreateReq;
import com.xiaozhi.common.model.resp.StackChanIdResp;
import com.xiaozhi.common.model.resp.StackChanPanoResp;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.service.StackChanPanoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * StackChan 固件/App 全景图接口（对应 Go 版后端 {@code api/pano/v1}）。
 * <p>
 * 设备身份由 {@link StackChanDeviceAuth} 确定，只能读写自己 MAC 名下的全景图。
 * 响应字段与 Go 版 JSON 对齐：{@code {id,panoUrl,createdAt,updatedAt}}。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 全景图接口", description = "固件/App 侧全景图上传与列表，设备身份由 Device-Id + HMAC token 确定")
public class StackChanPanoController {

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private StackChanPanoService stackChanPanoService;

    @Resource
    private DeviceService deviceService;

    @SaIgnore
    @PostMapping("/pano")
    @ResponseBody
    @Operation(summary = "上传全景图地址", description = "JSON 入参 {\"url\":\"...\"}；mac 以鉴权结果为准")
    public StackChanApiResponse<StackChanIdResp> addPano(@Valid @RequestBody StackChanPanoCreateReq req,
                                                         HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        requireDevice(mac);
        StackChanPanoBO pano = stackChanPanoService.create(mac, req.getUrl());
        StackChanIdResp data = new StackChanIdResp();
        data.setId(pano.getId());
        return StackChanApiResponse.success(data);
    }

    @SaIgnore
    @GetMapping("/pano")
    @ResponseBody
    @Operation(summary = "当前设备全景图列表", description = "按创建顺序返回，无数据时为空数组")
    public StackChanApiResponse<List<StackChanPanoResp>> listPano(HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        List<StackChanPanoBO> panos = stackChanPanoService.listByMac(mac);
        List<StackChanPanoResp> result = new ArrayList<>(panos.size());
        for (StackChanPanoBO pano : panos) {
            StackChanPanoResp resp = new StackChanPanoResp();
            resp.setId(pano.getId());
            resp.setMac(pano.getMac());
            resp.setPanoUrl(pano.getPanoUrl());
            resp.setCreatedTime(pano.getCreateTime());
            resp.setUpdatedTime(pano.getUpdateTime());
            result.add(resp);
        }
        return StackChanApiResponse.success(result);
    }

    /** Go 版后端 device_pano 表有指向 device 的外键，设备档案不存在时插入会失败，这里提前给明确报错 */
    private void requireDevice(String mac) {
        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            throw new IllegalArgumentException("The device does not exist or the Mac address is incorrect");
        }
    }
}
