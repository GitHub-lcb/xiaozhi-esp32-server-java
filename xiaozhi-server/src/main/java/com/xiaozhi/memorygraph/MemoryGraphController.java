package com.xiaozhi.memorygraph;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.req.MemoryRelationCreateReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.memorygraph.service.MemoryGraphService;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 记忆图谱：查看与手工维护实体关系。
 */
@RestController
@RequestMapping("/api/memory-graph")
@Tag(name = "记忆图谱", description = "实体关系查看与维护")
public class MemoryGraphController extends BaseController {

    @Resource
    private MemoryGraphService memoryGraphService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:memory-graph:api:list")
    @Operation(summary = "关系列表", description = "以「妈妈 喜欢 乌龙茶」的形式返回")
    public ApiResponse<List<String>> list(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ApiResponse.success(
            memoryGraphService.listRelationStatements(StpUtil.getLoginIdAsInt(), Math.min(limit, 200)));
    }

    @PostMapping("/relation")
    @ResponseBody
    @SaCheckPermission("system:memory-graph:api:create")
    @AuditLog(module = "记忆图谱", operation = "新增关系")
    @Operation(summary = "新增关系", description = "实体不存在时自动创建")
    public ApiResponse<Void> create(@Valid @RequestBody MemoryRelationCreateReq req) {
        memoryGraphService.addRelationByNames(StpUtil.getLoginIdAsInt(),
            req.getFromName(), req.getRelation(), req.getToName(), "person", null);
        return ApiResponse.success("添加成功");
    }
}
