package com.xiaozhi.operationlog;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.OperationLogBO;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.common.model.req.OperationLogPageReq;
import com.xiaozhi.common.model.resp.OperationLogResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.operationlog.convert.OperationLogConvert;
import com.xiaozhi.operationlog.service.OperationLogService;
import com.xiaozhi.server.web.BaseController;
import com.xiaozhi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志查询：审计数据在此之前只有写入与定时清理，管理端无法查看。
 */
@Slf4j
@RestController
@RequestMapping("/api/operation-log")
@Tag(name = "操作日志", description = "管理端操作审计日志查询")
public class OperationLogController extends BaseController {

    /** 审计表只会越来越大，单页上限 100，防止前端传大数把查询拖死 */
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private OperationLogConvert operationLogConvert;

    @Resource
    private UserService userService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:operation-log:api:list")
    @Operation(summary = "查询操作日志", description = "按模块、操作、用户、结果与时间范围分页查询审计日志")
    public ApiResponse<PageResult<OperationLogResp>> list(@Valid OperationLogPageReq req) {
        int pageSize = Math.min(Math.max(req.getPageSize(), 1), MAX_PAGE_SIZE);
        PageResult<OperationLogBO> page = operationLogService.page(req.getPageNo(), pageSize,
            req.getModule(), req.getOperation(), req.getUserId(), req.getSuccess(),
            req.getStartTime(), req.getEndTime());
        PageResult<OperationLogResp> result = page.map(operationLogConvert::toResp);
        fillUsernames(result.getList());
        return ApiResponse.success(result);
    }

    /** 审计表只存 userId，列表回显用户名；用户已删除时保留 userId，便于追溯。 */
    private void fillUsernames(List<OperationLogResp> list) {
        Map<Integer, String> names = new HashMap<>();
        for (OperationLogResp resp : list) {
            Integer userId = resp.getUserId();
            if (userId == null) {
                continue;
            }
            if (!names.containsKey(userId)) {
                String username = null;
                try {
                    UserBO user = userService.getBO(userId);
                    username = user == null ? null : user.getUsername();
                } catch (RuntimeException e) {
                    log.debug("查询操作日志的用户名失败, userId={}", userId, e);
                }
                names.put(userId, username);
            }
            resp.setUsername(names.get(userId));
        }
    }
}
