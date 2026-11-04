package com.xiaozhi.stackchan.store;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.StackChanDanceBO;
import com.xiaozhi.common.model.req.StackChanDanceCreateReq;
import com.xiaozhi.common.model.req.StackChanDanceDeleteReq;
import com.xiaozhi.common.model.req.StackChanDanceUpdateReq;
import com.xiaozhi.common.model.resp.StackChanDanceResp;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.service.StackChanDanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StackChan 固件侧跳舞接口，鉴权沿用固件已有的 Device-Id + HMAC token（{@link StackChanDeviceAuth}）。
 * <p>
 * 响应保持 Go 版后端的 {@code {"code":0,"message":"","data":...}}，固件不需要为新后端改解析逻辑。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 设备跳舞接口", description = "固件侧舞蹈增删改查，设备身份由 Device-Id + HMAC token 确定")
public class StackChanDanceController {

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private StackChanDanceService stackChanDanceService;

    @SaIgnore
    @GetMapping("/dance")
    @ResponseBody
    @Operation(summary = "查询当前设备的舞蹈列表")
    public StackChanApiResponse<List<StackChanDanceResp>> list(HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        List<StackChanDanceResp> dances = stackChanDanceService.listByMac(mac).stream()
            .map(this::toResp)
            .toList();
        return StackChanApiResponse.success(dances);
    }

    @SaIgnore
    @PostMapping("/dance")
    @ResponseBody
    @Operation(summary = "为当前设备新增舞蹈", description = "mac 以 Device-Id 为准，忽略请求体里的 mac")
    public StackChanApiResponse<String> create(@Valid @RequestBody StackChanDanceCreateReq req,
                                               HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        StackChanDanceBO bo = new StackChanDanceBO();
        bo.setMac(mac);
        applyCreateFields(req, bo);
        stackChanDanceService.create(bo);
        return StackChanApiResponse.success("Dance data saved successfully");
    }

    @SaIgnore
    @PutMapping("/dance")
    @ResponseBody
    @Operation(summary = "更新当前设备的舞蹈", description = "按 id 定位；只允许改本设备名下的舞蹈")
    public StackChanApiResponse<String> update(@Valid @RequestBody StackChanDanceUpdateReq req,
                                               HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        requireOwnedDance(req.getId(), mac);
        stackChanDanceService.update(req.getId(), toUpdateBO(req));
        return StackChanApiResponse.success("Update successful");
    }

    @SaIgnore
    @DeleteMapping("/dance")
    @ResponseBody
    @Operation(summary = "删除当前设备的舞蹈", description = "按 id 定位；只允许删本设备名下的舞蹈")
    public StackChanApiResponse<String> delete(@Valid @RequestBody StackChanDanceDeleteReq req,
                                               HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        requireOwnedDance(req.getId(), mac);
        stackChanDanceService.delete(req.getId());
        return StackChanApiResponse.success("Delete successful");
    }

    @SaIgnore
    @GetMapping("/danceData")
    @ResponseBody
    @Operation(summary = "查询舞蹈详情", description = "固件按 id 拉取单条舞蹈动作数据，只允许读本设备名下的舞蹈")
    public StackChanApiResponse<StackChanDanceResp> danceData(@RequestParam("id") Long id,
                                                              HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        StackChanDanceBO bo = stackChanDanceService.get(id);
        if (bo == null || !mac.equalsIgnoreCase(bo.getMac())) {
            throw new IllegalArgumentException("舞蹈不存在");
        }
        return StackChanApiResponse.success(toResp(bo));
    }

    /** 设备侧写接口按 id 操作，必须先确认这条舞蹈属于当前鉴权出来的设备 */
    private void requireOwnedDance(Long danceId, String mac) {
        StackChanDanceBO bo = stackChanDanceService.get(danceId);
        if (bo == null) {
            throw new IllegalArgumentException("舞蹈不存在");
        }
        if (!mac.equalsIgnoreCase(bo.getMac())) {
            throw new IllegalArgumentException("无权操作其他设备的舞蹈");
        }
    }

    private void applyCreateFields(StackChanDanceCreateReq req, StackChanDanceBO bo) {
        requireDanceArray(req.getDanceData());
        bo.setDanceName(req.getDanceName().trim());
        bo.setDanceData(req.getDanceData().toString());
        bo.setMusicUrl(StringUtils.hasText(req.getMusicUrl()) ? req.getMusicUrl().trim() : null);
    }

    private StackChanDanceBO toUpdateBO(StackChanDanceUpdateReq req) {
        StackChanDanceBO bo = new StackChanDanceBO();
        if (req.getDanceName() != null) {
            bo.setDanceName(req.getDanceName().trim());
        }
        if (req.getDanceData() != null) {
            requireDanceArray(req.getDanceData());
            bo.setDanceData(req.getDanceData().toString());
        }
        if (req.getMusicUrl() != null) {
            bo.setMusicUrl(req.getMusicUrl().trim());
        }
        return bo;
    }

    /** 固件/App 都按数组消费 danceData，对象或标量原样存进去只会在播放时炸 */
    private void requireDanceArray(com.fasterxml.jackson.databind.JsonNode danceData) {
        if (danceData == null || !danceData.isArray()) {
            throw new IllegalArgumentException("舞蹈动作数据必须是 JSON 数组");
        }
    }

    private StackChanDanceResp toResp(StackChanDanceBO bo) {
        StackChanDanceResp resp = new StackChanDanceResp();
        resp.setId(bo.getDanceId());
        resp.setMac(bo.getMac());
        resp.setDanceName(bo.getDanceName());
        resp.setMusicUrl(bo.getMusicUrl());
        resp.setDanceData(bo.getDanceData());
        return resp;
    }
}
