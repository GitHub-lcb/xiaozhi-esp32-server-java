package com.xiaozhi.stackchan.app.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 登录请求（Go 版 {@code POST /stackChan/v2/user/login} 的请求体）。
 */
@Data
@Schema(description = "StackChan App 登录请求")
public class StackChanLoginReq {

    @Schema(description = "用户名 / 邮箱 / 手机号", example = "demo")
    private String username;

    @Schema(description = "明文密码", example = "123456")
    private String password;
}
