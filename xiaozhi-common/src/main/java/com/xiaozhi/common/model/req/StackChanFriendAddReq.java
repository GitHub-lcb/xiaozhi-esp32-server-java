package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * StackChan 好友添加请求：friendMac 支持 12 位纯十六进制或带分隔符，服务端统一归一化。
 */
@Data
@Schema(description = "StackChan 好友添加请求")
public class StackChanFriendAddReq {

    @Schema(description = "好友设备 MAC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "friendMac cannot be empty")
    private String friendMac;
}
