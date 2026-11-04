package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 登录响应数据，对应 {@code app/lib/model/login_page.dart} 里的 {@code LoginResponseModel}。
 * <p>
 * token 是 Sa-Token 原值（不带 Bearer 前缀），App 会原样存本地并放进 {@code token} 请求头。
 */
@Data
@Schema(description = "StackChan App 登录响应")
public class StackChanLoginResp {

    @Schema(description = "Sa-Token 登录凭证")
    private String token;
}
