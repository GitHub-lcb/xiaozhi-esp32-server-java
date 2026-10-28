package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备截图记录分页查询")
public class DeviceScreenshotPageReq extends BasePageReq {

    @Schema(description = "设备ID，模糊匹配")
    private String deviceId;

    @Schema(description = "状态(0待回传 1成功 2失败)")
    private String state;
}
