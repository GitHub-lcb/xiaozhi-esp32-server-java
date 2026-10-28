package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "设备接入自检结果")
public class DeviceSelfCheckResp {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "总体结论：pass / warn / fail")
    private String summary;

    @Schema(description = "通过项数")
    private Integer passed;

    @Schema(description = "告警项数")
    private Integer warned;

    @Schema(description = "失败项数")
    private Integer failed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "自检时间")
    private LocalDateTime checkTime;

    @Schema(description = "逐项结果")
    private List<SelfCheckItemResp> items;
}
