package com.xiaozhi.stackchan.app.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 注册请求（Go 版 {@code POST /stackChan/v2/user/registration} 的请求体）。
 * <p>
 * App 不传邮箱验证码，服务端直接建号（自建后端没有 M5Stack 论坛账号体系）。
 */
@Data
@Schema(description = "StackChan App 注册请求")
public class StackChanRegistrationReq {

    @Schema(description = "用户名", example = "demo")
    private String username;

    @Schema(description = "明文密码", example = "123456")
    private String password;

    @Schema(description = "邮箱", example = "demo@example.com")
    private String email;
}
