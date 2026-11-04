package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志")
public class OperationLogResp {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作用户ID")
    private Integer userId;

    @Schema(description = "操作用户名（用户已被删除时为空）")
    private String username;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求 URL")
    private String url;

    @Schema(description = "处理类#方法")
    private String handler;

    @Schema(description = "请求参数（已脱敏，超过 2000 字截断）")
    private String params;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "耗时(毫秒)")
    private Integer costMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
