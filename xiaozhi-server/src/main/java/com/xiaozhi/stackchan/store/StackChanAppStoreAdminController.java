package com.xiaozhi.stackchan.store;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xiaozhi.common.model.bo.StackChanAppBO;
import com.xiaozhi.common.model.req.StackChanAppCreateReq;
import com.xiaozhi.common.model.req.StackChanAppDeleteReq;
import com.xiaozhi.common.model.req.StackChanAppUpdateReq;
import com.xiaozhi.common.model.resp.StackChanAppAdminResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.stackchan.service.StackChanAppStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StackChan App Store 管理端接口：维护固件 App Center 的应用条目。
 * <p>
 * 只要求登录（{@code @SaCheckLogin}），不新增权限键——管理端菜单/按钮权限等前端接入时再补种子，
 * 避免权限种子守卫与 db/init.sql 两头对不上导致管理员恒定 403。
 * 删除为软删除；公开列表（GET /api/stackChan/apps）只下发已上架且未删除的条目。
 */
@RestController
@RequestMapping("/api/stackChan/admin")
@Tag(name = "StackChan App Store 管理", description = "管理端 App Store 应用增删改查（软删除）")
public class StackChanAppStoreAdminController {

    @Resource
    private StackChanAppStoreService stackChanAppStoreService;

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @SaCheckLogin
    @GetMapping("/apps")
    @ResponseBody
    @Operation(summary = "应用列表", description = "未删除的全部条目，含下架项，按 appId 倒序")
    public ApiResponse<List<StackChanAppAdminResp>> list() {
        List<StackChanAppAdminResp> apps = stackChanAppStoreService.listAdminApps().stream()
            .map(this::toResp)
            .toList();
        return ApiResponse.success(apps);
    }

    @SaCheckLogin
    @PostMapping("/apps")
    @ResponseBody
    @Operation(summary = "新增应用", description = "appName 必填；appIconUrl/firmwareUrl 建议填 /api/stackChan/file/... 公开地址")
    public ApiResponse<StackChanAppAdminResp> create(@Valid @RequestBody StackChanAppCreateReq req) {
        StackChanAppBO bo = new StackChanAppBO();
        bo.setAppName(req.getAppName().trim());
        bo.setAppIconUrl(toStoredUrl(req.getAppIconUrl()));
        bo.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null);
        bo.setFirmwareUrl(toStoredUrl(req.getFirmwareUrl()));
        bo.setState(req.getState());
        return ApiResponse.success(toResp(stackChanAppStoreService.create(bo)));
    }

    @SaCheckLogin
    @PutMapping("/apps")
    @ResponseBody
    @Operation(summary = "更新应用", description = "body 传 id，其余字段为 null 表示不修改、空串表示清空")
    public ApiResponse<StackChanAppAdminResp> update(@Valid @RequestBody StackChanAppUpdateReq req) {
        StackChanAppBO bo = new StackChanAppBO();
        bo.setAppName(req.getAppName());
        bo.setAppIconUrl(toStoredUrl(req.getAppIconUrl()));
        bo.setDescription(req.getDescription());
        bo.setFirmwareUrl(toStoredUrl(req.getFirmwareUrl()));
        bo.setState(req.getState());
        return ApiResponse.success(toResp(stackChanAppStoreService.update(req.getId(), bo)));
    }

    @SaCheckLogin
    @DeleteMapping("/apps")
    @ResponseBody
    @Operation(summary = "删除应用", description = "软删除，body 传 id")
    public ApiResponse<Void> delete(@Valid @RequestBody StackChanAppDeleteReq req) {
        stackChanAppStoreService.delete(req.getId());
        return ApiResponse.success("删除成功");
    }

    /**
     * 兼容 Go 版存量的 {@code file/...} 相对路径：入库前补成 {@code /api/stackChan/file/...} 绝对地址，
     * 固件下载 App Store 安装包时会直接拿这个字段发起 HTTP 请求，相对路径在设备上解析不了。
     * 空串原样保留，表示「清空」；null 表示「未提供」。
     */
    private String toStoredUrl(String url) {
        if (url == null) {
            return null;
        }
        String value = url.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith("file/")) {
            return serverAddressProvider.getServerAddress() + "/api/stackChan/" + value;
        }
        return value;
    }

    private StackChanAppAdminResp toResp(StackChanAppBO bo) {
        StackChanAppAdminResp resp = new StackChanAppAdminResp();
        resp.setId(bo.getAppId());
        resp.setAppName(bo.getAppName());
        resp.setAppIconUrl(bo.getAppIconUrl());
        resp.setDescription(bo.getDescription());
        resp.setFirmwareUrl(bo.getFirmwareUrl());
        resp.setState(bo.getState());
        resp.setCreateTime(bo.getCreateTime());
        resp.setUpdateTime(bo.getUpdateTime());
        return resp;
    }
}
