package com.xiaozhi.stackchan.store;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.StackChanDanceBO;
import com.xiaozhi.common.model.req.StackChanDanceCreateReq;
import com.xiaozhi.common.model.req.StackChanDanceDeleteReq;
import com.xiaozhi.common.model.req.StackChanDanceUpdateReq;
import com.xiaozhi.common.model.resp.StackChanDanceResp;
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
 * StackChan App 侧跳舞接口（/api/stackChan/v2/dance 系列），字段与 Go 版后端一致。
 * <p>
 * 鉴权：{@code token} 头里带 Sa-Token（可带 Bearer 前缀）；账号/设备绑定接口由 App 兼容接口另一组控制器提供。
 */
@RestController
@RequestMapping("/api/stackChan/v2")
@Tag(name = "StackChan App 跳舞接口", description = "App 侧按 mac 管理舞蹈，token 头携带 Sa-Token")
public class StackChanAppDanceController {

    @Resource
    private StackChanAppAuth stackChanAppAuth;

    @Resource
    private StackChanDanceService stackChanDanceService;

    @SaIgnore
    @GetMapping("/dance")
    @ResponseBody
    @Operation(summary = "查询设备的舞蹈列表", description = "query 传 mac；mac 支持 12 位纯十六进制或带分隔符")
    public StackChanApiResponse<List<StackChanDanceResp>> list(@RequestParam("mac") String mac,
                                                               HttpServletRequest request) {
        stackChanAppAuth.requireLogin(request);
        List<StackChanDanceResp> dances = stackChanDanceService.listByMac(mac).stream()
            .map(this::toResp)
            .toList();
        return StackChanApiResponse.success(dances);
    }

    @SaIgnore
    @PostMapping("/dance")
    @ResponseBody
    @Operation(summary = "新增舞蹈", description = "body 必传 mac、danceName、danceData（JSON 数组原样存）")
    public StackChanApiResponse<String> create(@Valid @RequestBody StackChanDanceCreateReq req,
                                               HttpServletRequest request) {
        stackChanAppAuth.requireLogin(request);
        requireDanceArray(req.getDanceData());
        StackChanDanceBO bo = new StackChanDanceBO();
        bo.setMac(req.getMac());
        bo.setDanceName(req.getDanceName().trim());
        bo.setDanceData(req.getDanceData().toString());
        bo.setMusicUrl(StringUtils.hasText(req.getMusicUrl()) ? req.getMusicUrl().trim() : null);
        stackChanDanceService.create(bo);
        return StackChanApiResponse.success("Dance data saved successfully");
    }

    @SaIgnore
    @PutMapping("/dance")
    @ResponseBody
    @Operation(summary = "更新舞蹈", description = "body 传 id，其余字段不传表示不修改")
    public StackChanApiResponse<String> update(@Valid @RequestBody StackChanDanceUpdateReq req,
                                               HttpServletRequest request) {
        stackChanAppAuth.requireLogin(request);
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
        stackChanDanceService.update(req.getId(), bo);
        return StackChanApiResponse.success("Update successful");
    }

    @SaIgnore
    @DeleteMapping("/dance")
    @ResponseBody
    @Operation(summary = "删除舞蹈", description = "DELETE 请求体传 id（与 Go 版一致，App 当前这样调用）")
    public StackChanApiResponse<String> delete(@Valid @RequestBody StackChanDanceDeleteReq req,
                                               HttpServletRequest request) {
        stackChanAppAuth.requireLogin(request);
        stackChanDanceService.delete(req.getId());
        return StackChanApiResponse.success("Delete successful");
    }

    @SaIgnore
    @GetMapping("/danceData")
    @ResponseBody
    @Operation(summary = "查询舞蹈详情", description = "query 传 id")
    public StackChanApiResponse<StackChanDanceResp> danceData(@RequestParam("id") Long id,
                                                              HttpServletRequest request) {
        stackChanAppAuth.requireLogin(request);
        StackChanDanceBO bo = stackChanDanceService.get(id);
        if (bo == null) {
            throw new IllegalArgumentException("舞蹈不存在");
        }
        return StackChanApiResponse.success(toResp(bo));
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
