package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询。时间参数走 GET query，格式固定为 yyyy-MM-dd HH:mm:ss（与 MessagePageReq 一致）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询")
public class OperationLogPageReq extends BasePageReq {

    @Schema(description = "操作模块，模糊匹配")
    private String module;

    @Schema(description = "操作描述，模糊匹配")
    private String operation;

    @Schema(description = "操作用户ID")
    private Integer userId;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
