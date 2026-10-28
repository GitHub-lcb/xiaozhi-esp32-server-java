package com.xiaozhi.memoryfact;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.common.model.req.MemoryFactCreateReq;
import com.xiaozhi.common.model.req.MemoryFactPageReq;
import com.xiaozhi.common.model.resp.MemoryFactResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.memoryfact.convert.MemoryFactConvert;
import com.xiaozhi.memoryfact.service.MemoryFactService;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 长期记忆：人工维护"关于这个家庭应该记住的事"。
 */
@RestController
@RequestMapping("/api/memory-fact")
@Tag(name = "长期记忆", description = "长期记忆事实的维护")
@Slf4j
public class MemoryFactController extends BaseController {

    @Resource
    private MemoryFactService memoryFactService;

    @Resource
    private MemoryFactConvert memoryFactConvert;

    @Resource
    private com.xiaozhi.ai.memory.MemoryFactExtractor memoryFactExtractor;

    @Resource
    private com.xiaozhi.ai.memory.MemoryGraphExtractor memoryGraphExtractor;

    @Resource
    private com.xiaozhi.memorygraph.service.MemoryGraphService memoryGraphService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:memory-fact:api:list")
    @Operation(summary = "记忆列表", description = "按内容与成员筛选")
    public ApiResponse<PageResult<MemoryFactResp>> list(@Valid MemoryFactPageReq req) {
        return ApiResponse.success(memoryFactService
            .page(req.getPageNo(), req.getPageSize(), req.getContent(), req.getMemberName(),
                StpUtil.getLoginIdAsInt())
            .map(memoryFactConvert::toResp));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:memory-fact:api:create")
    @AuditLog(module = "长期记忆", operation = "新增记忆")
    @Operation(summary = "新增记忆", description = "不填成员表示对全家人有效")
    public ApiResponse<MemoryFactResp> create(@Valid @RequestBody MemoryFactCreateReq req) {
        MemoryFactBO bo = new MemoryFactBO();
        bo.setUserId(StpUtil.getLoginIdAsInt());
        bo.setMemberName(req.getMemberName());
        bo.setContent(req.getContent());
        bo.setCategory(req.getCategory());
        bo.setSource(MemoryFactBO.SOURCE_MANUAL);
        MemoryFactBO created = memoryFactService.create(bo);
        indexGraph(created);
        return ApiResponse.success(memoryFactConvert.toResp(created));
    }

    /**
     * 从设备最近一段对话里自动提炼事实（source=auto）
     */
    @PostMapping("/extract")
    @ResponseBody
    @SaCheckPermission("system:memory-fact:api:create")
    @AuditLog(module = "长期记忆", operation = "自动抽取记忆")
    @Operation(summary = "自动抽取记忆", description = "从指定设备最近 30 条对话里提炼事实并去重入库")
    public ApiResponse<java.util.List<MemoryFactResp>> extract(@RequestParam("deviceId") String deviceId,
                                                               @RequestParam("roleId") Integer roleId,
                                                               @RequestParam(value = "memberName", required = false)
                                                               String memberName) {
        return ApiResponse.success(memoryFactExtractor
            .extract(StpUtil.getLoginIdAsInt(), deviceId, roleId, memberName).stream()
            .map(memoryFactConvert::toResp)
            .toList());
    }

    /**
     * 顺手把事实抽成图谱三元组；抽不出来（句式不匹配）就跳过，不影响事实本身。
     */
    private void indexGraph(MemoryFactBO fact) {
        try {
            memoryGraphExtractor.extract(fact.getContent()).ifPresent(triple ->
                memoryGraphService.addRelationByNames(fact.getUserId(), triple.from(), triple.relation(),
                    triple.to(), "person", null));
        } catch (RuntimeException e) {
            log.warn("事实转图谱失败，忽略 factId={}", fact.getFactId(), e);
        }
    }

    @DeleteMapping("/{factId}")
    @ResponseBody
    @SaCheckPermission("system:memory-fact:api:delete")
    @AuditLog(module = "长期记忆", operation = "删除记忆")
    @Operation(summary = "删除记忆", description = "只能删除自己的记忆")
    public ApiResponse<Void> delete(@PathVariable Long factId) {
        memoryFactService.delete(factId, StpUtil.getLoginIdAsInt());
        return ApiResponse.success("删除成功");
    }
}
