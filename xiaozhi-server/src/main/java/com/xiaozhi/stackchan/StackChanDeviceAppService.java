package com.xiaozhi.stackchan;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.device.DeviceAppService;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.model.resp.StackChanAppResp;
import com.xiaozhi.stackchan.model.resp.StackChanDeviceInfoResp;
import com.xiaozhi.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * StackChan 固件设备侧接口的业务门面。
 * <p>
 * P0 只覆盖开源固件实际调用的 4 个接口：
 * {@code /stackChan/device/user}、{@code /device/info}、{@code /device/unbind}、{@code /apps}。
 * 数据直接复用 Java 现有的设备/用户模型，不引入新的表。
 */
@Slf4j
@Service
public class StackChanDeviceAppService {

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private DeviceService deviceService;

    @Resource
    private UserService userService;

    @Resource
    private DeviceAppService deviceAppService;

    /** 固件 Setup 页展示的账号名 */
    public String getDeviceUsername(HttpServletRequest request) {
        DeviceBO device = requireDevice(request);
        if (device.getUserId() == null) {
            return "";
        }
        UserBO user = userService.getBO(device.getUserId());
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return "";
        }
        return user.getUsername();
    }

    /** 固件 Setup 页展示的设备名 */
    public StackChanDeviceInfoResp getDeviceInfo(HttpServletRequest request) {
        DeviceBO device = requireDevice(request);
        StackChanDeviceInfoResp resp = new StackChanDeviceInfoResp();
        resp.setName(device.getDeviceName());
        return resp;
    }

    /**
     * 固件侧解除绑定：删除设备档案，下次 OTA 会按家庭自用模式重新自动绑定。
     */
    public boolean unbind(HttpServletRequest request) {
        String deviceId = stackChanDeviceAuth.authenticate(request);
        if (deviceService.getBO(deviceId) == null) {
            return true;
        }
        deviceAppService.delete(deviceId);
        log.info("StackChan 设备已解绑, deviceId={}", deviceId);
        return true;
    }

    /**
     * App Center 列表：P0 返回空列表，迁移 App Store 后再接数据。
     */
    public List<StackChanAppResp> listApps() {
        return List.of();
    }

    private DeviceBO requireDevice(HttpServletRequest request) {
        String deviceId = stackChanDeviceAuth.authenticate(request);
        DeviceBO device = deviceService.getBO(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return device;
    }
}
