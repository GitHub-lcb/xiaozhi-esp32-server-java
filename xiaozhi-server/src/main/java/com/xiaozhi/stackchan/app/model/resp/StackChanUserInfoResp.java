package com.xiaozhi.stackchan.app.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App 用户信息响应，字段严格对应 {@code app/lib/model/user_info.dart} 的 {@code UserInfo.fromJson}。
 * <p>
 * 注意这里是 camelCase（displayName/iconText/iconBgColor/joinDate/lastOnline），
 * 与注册接口返回的 NodeBB 风格字段名（displayname/icon:text/joindate...）不是同一套。
 */
@Data
@Schema(description = "StackChan App 用户信息")
public class StackChanUserInfoResp {

    @Schema(description = "用户 ID")
    private Integer uid;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "用户别名（NodeBB userslug 风格）")
    private String userslug;

    @Schema(description = "展示名")
    private String displayName;

    @Schema(description = "头像文字（展示名首字符）")
    private String iconText;

    @Schema(description = "头像背景色，#RRGGBB")
    private String iconBgColor;

    @Schema(description = "邮箱是否已验证：0-否 1-是")
    private Integer emailConfirmed;

    @Schema(description = "注册时间，毫秒时间戳")
    private Long joinDate;

    @Schema(description = "最近在线时间，毫秒时间戳")
    private Long lastOnline;

    @Schema(description = "在线状态")
    private String userStatus;
}
