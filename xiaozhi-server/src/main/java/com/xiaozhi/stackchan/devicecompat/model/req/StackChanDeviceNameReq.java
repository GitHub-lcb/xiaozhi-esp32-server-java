package com.xiaozhi.stackchan.devicecompat.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 设备 v1 改名请求（Go 版 {@code api/device/v1} 的 CreateReq/UpdateReq/UpdateDeviceInfoReq）。
 * <p>
 * 三个接口的请求体都只有 {@code name} 一个字段；为空时按 Go 语义视为不修改。
 */
@Data
@Schema(description = "StackChan 设备改名请求")
public class StackChanDeviceNameReq {

    @Schema(description = "新的设备名称，为空表示不修改", example = "Living Room StackChan")
    private String name;
}
