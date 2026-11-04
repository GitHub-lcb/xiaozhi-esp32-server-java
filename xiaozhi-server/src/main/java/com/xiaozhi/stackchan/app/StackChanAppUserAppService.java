package com.xiaozhi.stackchan.app;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.exception.UnauthorizedException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.common.web.TrustedProxyPolicy;
import com.xiaozhi.security.service.AuthenticationService;
import com.xiaozhi.stackchan.app.model.req.StackChanLoginReq;
import com.xiaozhi.stackchan.app.model.req.StackChanRegistrationReq;
import com.xiaozhi.stackchan.app.model.resp.StackChanLoginResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanRegistrationResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanUserInfoResp;
import com.xiaozhi.user.UserAppService;
import com.xiaozhi.user.service.UserService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * StackChan App 兼容接口的用户侧编排：登录、注册、用户信息。
 * <p>
 * 登录/注册复用管理端已有能力（{@link UserAppService} 的密码校验与默认角色/模板复制），
 * 只把返回结构适配成 Go 版 App 解析的 NodeBB 风格字段。
 */
@Slf4j
@Service
public class StackChanAppUserAppService {

    /** App 登录页对这两个消息做了专门弹窗，其余错误只弹 toast */
    private static final String INVALID_CREDENTIALS_MESSAGE = "[[error:invalid-login-credentials]]";

    private static final String ACCOUNT_DISABLED_MESSAGE = "This account has been disabled, please contact the administrator.";

    /** 必须与 App 注册页的字符串判断保持一致：code==300 且 message 含该句时展示用户名输入框内联错误 */
    private static final String USERNAME_TAKEN_MESSAGE =
        "Username is already registered or unavailable, please change the username and try again.";

    private static final String EMAIL_TAKEN_MESSAGE =
        "This email address has already been registered, please use another one.";

    private static final String REGISTRATION_FAILED_MESSAGE =
        "Registration failed, please check the username, password and email and try again.";

    private static final String SERVER_NOT_READY_MESSAGE =
        "Registration failed because the server could not initialize the account "
            + "(the admin default role/template may be missing). Please contact the administrator.";

    private static final String USER_STATUS_ONLINE = "online";

    /** 自建后端没有邮件确认环节，账号注册后即可登录，如实返回 0（App 资料页显示 Unverified） */
    private static final int EMAIL_NOT_CONFIRMED = 0;

    private static final int USERNAME_MAX_LENGTH = 50;

    private static final int EMAIL_MAX_LENGTH = 100;

    /** NodeBB 头像背景色：App 端把该值当 #RRGGBB 解析，这里按 userId 取稳定色 */
    private static final String[] ICON_BACKGROUND_COLORS = {
        "#f56c6c", "#e6a23c", "#67c23a", "#409eff", "#909399",
        "#9c27b0", "#00bcd4", "#ff9800", "#795548", "#607d8b"
    };

    @Resource
    private UserAppService userAppService;

    @Resource
    private UserService userService;

    @Resource
    private AuthenticationService authenticationService;

    @Resource
    private TrustedProxyPolicy trustedProxyPolicy;

    /**
     * App 登录：复用管理端登录校验，成功后签发同一个 Sa-Token 返回给 App。
     */
    public StackChanLoginResp login(StackChanLoginReq req, HttpServletRequest request) {
        if (req == null || !StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw StackChanAppException.business("Username / Password cannot be left blank.");
        }

        UserBO user;
        try {
            user = userAppService.login(req.getUsername().trim(), req.getPassword());
        } catch (UsernameNotFoundException | UserPasswordNotMatchException e) {
            // App 登录页只在 message 等于该标记时展示"账号或密码错误"弹窗，其余文案走 toast
            throw StackChanAppException.business(INVALID_CREDENTIALS_MESSAGE);
        } catch (UnauthorizedException e) {
            throw StackChanAppException.business(ACCOUNT_DISABLED_MESSAGE);
        }

        userAppService.recordLoginInfo(user, trustedProxyPolicy.resolveClientIp(request));

        StpUtil.login(user.getUserId(), userAppService.getTokenExpireSeconds());
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token)) {
            throw StackChanAppException.internal("Login succeeded but the token could not be issued.");
        }
        log.info("StackChan App 登录成功, userId={}", user.getUserId());

        StackChanLoginResp resp = new StackChanLoginResp();
        resp.setToken(token);
        return resp;
    }

    /**
     * App 注册：App 不传邮箱验证码，这里直接建号，并复制管理端的默认角色/模板（家庭自用场景）。
     */
    public StackChanRegistrationResp register(StackChanRegistrationReq req) {
        if (req == null
            || !StringUtils.hasText(req.getUsername())
            || !StringUtils.hasText(req.getPassword())
            || !StringUtils.hasText(req.getEmail())) {
            throw StackChanAppException.business("Username / Email / Password cannot be empty.");
        }

        String username = req.getUsername().trim();
        String email = req.getEmail().trim();
        if (username.length() > USERNAME_MAX_LENGTH) {
            throw StackChanAppException.business("The username is too long (max " + USERNAME_MAX_LENGTH + " characters).");
        }
        if (!email.contains("@") || email.length() > EMAIL_MAX_LENGTH) {
            throw StackChanAppException.business("Please enter a valid email address.");
        }
        if (userService.getByUsername(username) != null) {
            throw StackChanAppException.business(USERNAME_TAKEN_MESSAGE);
        }
        if (userService.getByEmail(email) != null) {
            throw StackChanAppException.business(EMAIL_TAKEN_MESSAGE);
        }

        UserBO user = new UserBO();
        user.setUsername(username);
        user.setPassword(authenticationService.encryptPassword(req.getPassword()));
        user.setName(username);
        user.setEmail(email);

        UserBO created;
        try {
            created = userAppService.createUserWithDefaults(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名/同邮箱时才会走到这里，唯一键冲突没法区分是哪一列
            log.warn("StackChan App 注册唯一键冲突, username={}", username, e);
            throw StackChanAppException.business(EMAIL_TAKEN_MESSAGE);
        } catch (IllegalArgumentException e) {
            log.warn("StackChan App 注册参数异常, username={}", username, e);
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("用户名")) {
                throw StackChanAppException.business(USERNAME_TAKEN_MESSAGE);
            }
            if (message.contains("邮箱")) {
                throw StackChanAppException.business(EMAIL_TAKEN_MESSAGE);
            }
            throw StackChanAppException.business(REGISTRATION_FAILED_MESSAGE);
        } catch (IllegalStateException e) {
            // copyDefaultRole 在 admin（userId=1）没有默认角色时抛"默认角色模板不存在"
            log.error("StackChan App 注册失败（默认角色/模板初始化异常）, username={}", username, e);
            throw StackChanAppException.internal(SERVER_NOT_READY_MESSAGE);
        }

        log.info("StackChan App 注册成功, userId={}, username={}", created.getUserId(), created.getUsername());
        return toRegistrationResp(created);
    }

    /**
     * 当前登录用户信息，字段按 {@code app/lib/model/user_info.dart} 输出。
     */
    public StackChanUserInfoResp getUserInfo(Integer userId) {
        UserBO user = userService.getBO(userId);
        if (user == null) {
            // 账号已删除但 token 还没过期：按未登录处理，App 收到 401 会清本地登录态
            throw StackChanAppException.unauthorized("The user in the token does not exist.");
        }

        String displayName = displayNameOf(user);
        StackChanUserInfoResp resp = new StackChanUserInfoResp();
        resp.setUid(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserslug(userslugOf(user.getUsername(), user.getUserId()));
        resp.setDisplayName(displayName);
        resp.setIconText(iconTextOf(displayName));
        resp.setIconBgColor(iconBgColorOf(user.getUserId()));
        resp.setEmailConfirmed(EMAIL_NOT_CONFIRMED);
        resp.setJoinDate(toEpochMillis(user.getCreateTime()));
        resp.setLastOnline(toEpochMillis(user.getLoginTime() != null ? user.getLoginTime() : user.getCreateTime()));
        resp.setUserStatus(USER_STATUS_ONLINE);
        return resp;
    }

    private StackChanRegistrationResp toRegistrationResp(UserBO user) {
        String displayName = displayNameOf(user);
        long joinedAt = toEpochMillis(user.getCreateTime());

        StackChanRegistrationResp resp = new StackChanRegistrationResp();
        resp.setUid(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserslug(userslugOf(user.getUsername(), user.getUserId()));
        resp.setEmail(user.getEmail());
        resp.setEmailConfirmed(EMAIL_NOT_CONFIRMED);
        resp.setJoinDate(joinedAt);
        resp.setLastOnline(joinedAt);
        resp.setIconBgColor(iconBgColorOf(user.getUserId()));
        resp.setDisplayname(displayName);
        resp.setIconText(iconTextOf(displayName));
        resp.setStatus(USER_STATUS_ONLINE);
        return resp;
    }

    private String displayNameOf(UserBO user) {
        if (StringUtils.hasText(user.getName())) {
            return user.getName();
        }
        return user.getUsername();
    }

    /** NodeBB userslug：小写、非字母数字折叠成 '-'；纯中文用户名兜底成 user-{uid}，避免空串 */
    private String userslugOf(String username, Integer userId) {
        String slug = "";
        if (StringUtils.hasText(username)) {
            slug = username.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        }
        if (StringUtils.hasText(slug)) {
            return slug;
        }
        return userId == null ? "user" : "user-" + userId;
    }

    private String iconTextOf(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "?";
        }
        String trimmed = displayName.trim();
        int codePoint = trimmed.codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT);
    }

    private String iconBgColorOf(Integer userId) {
        int index = userId == null ? 0 : Math.floorMod(userId, ICON_BACKGROUND_COLORS.length);
        return ICON_BACKGROUND_COLORS[index];
    }

    /** DATETIME（应用时区墙上时间）→ App 要的毫秒时间戳；缺失时用当前时间兜底 */
    private long toEpochMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return DateUtils.millis();
        }
        Instant instant = DateUtils.toInstant(dateTime);
        return instant == null ? DateUtils.millis() : instant.toEpochMilli();
    }
}
