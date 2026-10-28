package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "设备远程控制指令记录")
public class DeviceCommandResp {

    @Schema(description = "指令ID")
    private Long commandId;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "调用的设备工具")
    private String toolName;

    @Schema(description = "工具入参 JSON")
    private String arguments;

    @Schema(description = "状态(0已下发 1成功 2失败)")
    private String state;

    @Schema(description = "执行结果")
    private String result;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下发时间")
    private LocalDateTime createTime;
}
