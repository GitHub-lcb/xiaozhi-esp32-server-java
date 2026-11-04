package com.xiaozhi.stackchan.app.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 只带设备 MAC 的请求体：绑定 / 激活绑定 / 解绑共用。
 * <p>
 * App 传的是 12 位纯 MAC（大小写均可能），服务端统一 normalize 成小写冒号格式再落库。
 */
@Data
@Schema(description = "StackChan 设备 MAC 请求")
public class StackChanMacReq {

    @Schema(description = "设备 MAC，如 aabbccddeeff 或 AA:BB:CC:DD:EE:FF", example = "aabbccddeeff")
    private String mac;
}
