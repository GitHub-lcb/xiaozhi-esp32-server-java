package com.xiaozhi.stackchan.app.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 设备列表项，字段严格对应 {@code app/lib/model/device.dart}。
 * <p>
 * mac 返回 12 位小写纯 hex（无冒号），因为 App 的 deviceMac 是纯 MAC；
 * 库里的 deviceId 是小写冒号格式，由服务端在出入口双向转换。
 */
@Data
@Schema(description = "StackChan App 设备信息")
public class StackChanDeviceResp {

    @Schema(description = "设备 MAC，12 位小写纯 hex", example = "aabbccddeeff")
    private String mac;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "绑定用户 ID")
    private Integer uid;

    @JsonProperty("bind_time")
    @Schema(description = "绑定时间，格式 yyyy-MM-dd HH:mm:ss")
    private String bindTime;
}
