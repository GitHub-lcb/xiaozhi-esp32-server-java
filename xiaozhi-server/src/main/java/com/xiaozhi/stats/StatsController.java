package com.xiaozhi.stats;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 监控面板：概览、消息趋势、Top 设备。
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "监控面板", description = "设备活跃度与消息量统计")
public class StatsController extends BaseController {

    @Resource
    private StatsAppService statsAppService;

    @Resource
    private com.xiaozhi.stats.service.DialogueMetricService dialogueMetricService;

    @GetMapping("/overview")
    @ResponseBody
    @SaCheckPermission("system:stats:api:overview")
    @Operation(summary = "概览", description = "设备总数/在线数、今日与近 7 天消息量、活跃设备数")
    public ApiResponse<Object> overview() {
        return ApiResponse.success(statsAppService.overview(StpUtil.getLoginIdAsInt()));
    }

    @GetMapping("/dialogue")
    @ResponseBody
    @SaCheckPermission("system:stats:api:overview")
    @Operation(summary = "对话指标", description = "最近 N 天的轮次、token 合计与平均时延")
    public ApiResponse<Object> dialogue(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        int span = Math.min(Math.max(days, 1), 90);
        java.time.LocalDateTime from = com.xiaozhi.utils.DateUtils.today().minusDays(span - 1L).atStartOfDay();
        return ApiResponse.success(dialogueMetricService.summary(from));
    }

    @GetMapping("/messages/daily")
    @ResponseBody
    @SaCheckPermission("system:stats:api:messages")
    @Operation(summary = "消息趋势", description = "最近 N 天每天的消息量，缺省补 0")
    public ApiResponse<Object> dailyMessages(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ApiResponse.success(statsAppService.dailyMessages(days));
    }

    @GetMapping("/devices/top")
    @ResponseBody
    @SaCheckPermission("system:stats:api:devices")
    @Operation(summary = "Top 设备", description = "最近 N 天消息量最多的设备")
    public ApiResponse<Object> topDevices(
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ApiResponse.success(statsAppService.topDevices(days, limit));
    }
}
