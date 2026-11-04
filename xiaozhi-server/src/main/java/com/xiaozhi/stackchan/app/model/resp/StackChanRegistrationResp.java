package com.xiaozhi.stackchan.app.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 注册响应，字段严格对应 {@code app/lib/model/registration_response.dart}。
 * <p>
 * NodeBB 的字段名带冒号（email:confirmed、icon:bgColor、icon:text），
 * 时间字段叫 joindate/lastonline（全小写、毫秒时间戳），不能用 Java 驼峰直接序列化。
 */
@Data
@Schema(description = "StackChan App 注册响应")
public class StackChanRegistrationResp {

    @Schema(description = "用户 ID")
    private Integer uid;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "用户别名")
    private String userslug;

    @Schema(description = "邮箱")
    private String email;

    @JsonProperty("email:confirmed")
    @Schema(description = "邮箱是否已验证：0-否 1-是")
    private Integer emailConfirmed;

    @JsonProperty("joindate")
    @Schema(description = "注册时间，毫秒时间戳")
    private Long joinDate;

    @JsonProperty("lastonline")
    @Schema(description = "最近在线时间，毫秒时间戳")
    private Long lastOnline;

    @Schema(description = "头像地址，自建后端暂无")
    private Object picture;

    @JsonProperty("icon:bgColor")
    @Schema(description = "头像背景色")
    private String iconBgColor;

    @Schema(description = "全名，自建后端暂无")
    private Object fullname;

    @Schema(description = "展示名")
    private String displayname;

    @JsonProperty("icon:text")
    @Schema(description = "头像文字")
    private String iconText;

    @Schema(description = "用户状态")
    private String status;
}
