package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "设备截图记录")
public class DeviceScreenshotResp {

    @Schema(description = "截图ID")
    private Long shotId;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "关联指令ID")
    private Long commandId;

    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "状态(0待回传 1成功 2失败)")
    private String state;

    @Schema(description = "失败原因或摘要")
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
