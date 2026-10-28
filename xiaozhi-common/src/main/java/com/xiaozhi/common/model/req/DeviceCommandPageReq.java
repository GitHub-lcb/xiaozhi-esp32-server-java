package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备指令记录分页查询")
public class DeviceCommandPageReq extends BasePageReq {

    @Schema(description = "设备ID，模糊匹配")
    private String deviceId;

    @Schema(description = "状态(0已下发 1成功 2失败)")
    private String state;
}
