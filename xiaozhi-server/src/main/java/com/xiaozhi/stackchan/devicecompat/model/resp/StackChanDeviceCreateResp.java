package com.xiaozhi.stackchan.devicecompat.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 设备 v1 建档响应（对应 Go 版 {@code CreateRes{Id int64}}）。
 * <p>
 * Go 版 {@code device} 表有自增行 id；Java 侧 {@code sys_device} 的主键是字符串 {@code deviceId}，
 * 没有可等价返回的数字 id，因此固定返回 0，表示"设备已存在、本次请求已幂等处理"。
 */
@Data
@Schema(description = "StackChan 设备建档响应")
public class StackChanDeviceCreateResp {

    @Schema(description = "设备行 id；Java 侧无自增 id，固定为 0", example = "0")
    private Long id;
}
