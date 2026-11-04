package com.xiaozhi.stackchan.app.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 修改设备名请求（Go 版 {@code PUT /stackChan/v2/device/update}）。
 * <p>
 * Go 版请求体还带 longitude/latitude，本仓库设备表没有对应列，这里只声明 mac/name；
 * Spring Boot 默认忽略未知字段，App 多传经纬度也不会 400。
 */
@Data
@Schema(description = "StackChan App 修改设备名请求")
public class StackChanDeviceUpdateReq {

    @Schema(description = "设备 MAC", example = "aabbccddeeff")
    private String mac;

    @Schema(description = "新设备名，为空表示不修改", example = "My StackChan")
    private String name;
}
