package com.xiaozhi.stackchan;

import com.xiaozhi.communication.auth.DeviceAuthService;
import com.xiaozhi.utils.CommonUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * StackChan 设备侧鉴权：复用 xiaozhi 设备 token（HMAC-SHA256），
 * 设备在 {@code Authorization} 里带 token，在 {@code Device-Id} 里带 MAC。
 * <p>
 * 服务端未配置 {@code xiaozhi.device-auth.secret} 时等同于不鉴权（与 xiaozhi WS 保持一致），
 * 此时至少要求带 Device-Id 用于定位设备。
 */
@Component
public class StackChanDeviceAuth {

    @Resource
    private DeviceAuthService deviceAuthService;

    /**
     * 校验设备请求并返回小写冒号格式的 MAC。
     *
     * @throws IllegalArgumentException Device-Id 缺失/非法，或 token 校验失败
     */
    public String authenticate(HttpServletRequest request) {
        String rawDeviceId = request.getHeader("Device-Id");
        if (!StringUtils.hasText(rawDeviceId)) {
            throw new IllegalArgumentException("缺少 Device-Id");
        }
        String deviceId = rawDeviceId.trim().toLowerCase(Locale.ROOT).replace('-', ':');
        if (!CommonUtils.isMacAddressValid(deviceId)) {
            throw new IllegalArgumentException("设备ID不正确");
        }
        if (!deviceAuthService.isEnabled() || deviceAuthService.isAllowedDevice(deviceId)) {
            return deviceId;
        }
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("缺少设备 token");
        }
        token = token.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (!deviceAuthService.verifyDeviceToken(token, deviceId)) {
            throw new IllegalArgumentException("设备 token 无效");
        }
        return deviceId;
    }
}
