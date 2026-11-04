package com.xiaozhi.stackchan.app;

import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.device.DeviceAppService;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.stackchan.app.model.req.StackChanDeviceUpdateReq;
import com.xiaozhi.stackchan.app.model.req.StackChanMacReq;
import com.xiaozhi.stackchan.app.model.resp.StackChanDeviceResp;
import com.xiaozhi.utils.CommonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * StackChan App 兼容接口的设备侧编排：列表、绑定/激活绑定、改名、解绑、恢复默认。
 * <p>
 * 设备内部统一使用 xiaozhi 的 deviceId（小写冒号 MAC）；App 只认 12 位小写纯 hex，
 * 所有出入口都在本类做转换。家庭自用模式下"激活"等价于"建档并绑定到当前用户的默认角色"。
 */
@Slf4j
@Service
public class StackChanAppDeviceAppService {

    private static final String DEFAULT_DEVICE_NAME = "StackChan";

    /** 通过 App 首次建档的设备类型，与固件 OTA 上报的 board.type 同族，便于识别 */
    private static final String STACKCHAN_DEVICE_TYPE = "stackchan";

    private static final int DEVICE_NAME_MAX_LENGTH = 100;

    private static final DateTimeFormatter BIND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceAppService deviceAppService;

    @Resource
    private RoleService roleService;

    /**
     * 当前用户名下可被 App 管理的设备。
     * <p>
     * 注册时 createUserWithDefaults 会建一个 {@code user_chat_{uid}} 的网页聊天虚拟设备，
     * 它不是真实 MAC，App 无法切换/连接，必须过滤掉（normalizeMac 对非 MAC 返回 null）。
     */
    public List<StackChanDeviceResp> listDevices(Integer userId) {
        List<StackChanDeviceResp> result = new ArrayList<>();
        for (DeviceBO device : deviceService.listByUserId(userId)) {
            String mac = normalizeMac(device.getDeviceId());
            if (mac == null) {
                continue;
            }
            StackChanDeviceResp resp = new StackChanDeviceResp();
            resp.setMac(toPureMac(mac));
            resp.setName(StringUtils.hasText(device.getDeviceName()) ? device.getDeviceName() : "");
            resp.setUid(userId);
            resp.setBindTime(formatBindTime(device.getCreateTime()));
            result.add(resp);
        }
        return result;
    }

    /**
     * 绑定设备：已绑定当前用户则幂等成功；绑在别人名下报错；不存在则用当前用户默认角色建档并绑定。
     * <p>
     * activate_bind 与 bind 在自建后端语义相同（没有 M5Stack 云激活流程），共用本方法。
     */
    public Boolean bindDevice(Integer userId, StackChanMacReq req) {
        String mac = requireMac(req == null ? null : req.getMac());

        DeviceBO existing = deviceService.getBO(mac);
        if (existing != null) {
            if (userId.equals(existing.getUserId())) {
                return Boolean.TRUE;
            }
            throw StackChanAppException.business(
                "This device has already been bound to another account. Please unbind it from that account first.");
        }

        Integer roleId = requireDefaultRoleId(userId);
        deviceService.register(mac, DEFAULT_DEVICE_NAME, STACKCHAN_DEVICE_TYPE, userId, roleId);
        log.info("StackChan App 绑定设备成功, userId={}, deviceId={}, roleId={}", userId, mac, roleId);
        return Boolean.TRUE;
    }

    /** 修改设备名，只改 sys_device.deviceName；name 为空时按 Go 版语义视为不修改并返回成功 */
    public Boolean updateDevice(Integer userId, StackChanDeviceUpdateReq req) {
        String mac = requireMac(req == null ? null : req.getMac());
        requireOwnedDevice(userId, mac);

        if (!StringUtils.hasText(req.getName())) {
            return Boolean.TRUE;
        }
        String name = req.getName().trim();
        if (name.length() > DEVICE_NAME_MAX_LENGTH) {
            throw StackChanAppException.business(
                "The device name is too long (max " + DEVICE_NAME_MAX_LENGTH + " characters).");
        }

        DeviceUpdateReq updateReq = new DeviceUpdateReq();
        updateReq.setDeviceName(name);
        deviceAppService.update(mac, updateReq);
        log.info("StackChan App 修改设备名成功, userId={}, deviceId={}, name={}", userId, mac, name);
        return Boolean.TRUE;
    }

    /**
     * 解绑：删除设备档案（与 P0 固件侧 /device/unbind 一致，设备下次 OTA 会按家庭自用模式重新建档）。
     * 设备已不存在时幂等返回成功，与 Go 版一致。
     */
    public Boolean unbindDevice(Integer userId, StackChanMacReq req) {
        String mac = requireMac(req == null ? null : req.getMac());

        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            return Boolean.TRUE;
        }
        if (!userId.equals(device.getUserId())) {
            throw StackChanAppException.business("Device not found or not bound to the current account.");
        }
        try {
            deviceAppService.delete(mac);
            log.info("StackChan App 解绑设备成功, userId={}, deviceId={}", userId, mac);
        } catch (ResourceNotFoundException e) {
            // 并发解绑：另一个请求已经删掉了，按幂等成功处理
            log.warn("StackChan App 解绑时设备已不存在, userId={}, deviceId={}", userId, mac);
        }
        return Boolean.TRUE;
    }

    /**
     * 恢复默认 Agent：P0 占位，App 后续不使用。
     */
    public Boolean restoreDefaultAgent() {
        return Boolean.TRUE;
    }

    private DeviceBO requireOwnedDevice(Integer userId, String mac) {
        DeviceBO device = deviceService.getBO(mac);
        if (device == null || !userId.equals(device.getUserId())) {
            throw StackChanAppException.business("Device not found or not bound to the current account.");
        }
        return device;
    }

    private Integer requireDefaultRoleId(Integer userId) {
        RoleBO role = roleService.getDefaultOrFirstBO(userId);
        if (role == null || role.getRoleId() == null) {
            throw StackChanAppException.internal(
                "No default role is configured for this account. Please create a role in the admin console first.");
        }
        return role.getRoleId();
    }

    /**
     * 把 App 传来的 MAC 归一化成内部 deviceId（小写冒号格式）。
     * 支持 12 位纯 hex、冒号分隔、短横线分隔，大小写不限；非 MAC（如 user_chat_1）返回 null。
     */
    private String normalizeMac(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String compact = raw.trim().replace("-", "").replace(":", "");
        if (compact.length() != 12) {
            return null;
        }
        for (int i = 0; i < compact.length(); i++) {
            if (Character.digit(compact.charAt(i), 16) < 0) {
                return null;
            }
        }

        String lower = compact.toLowerCase(Locale.ROOT);
        StringBuilder mac = new StringBuilder(17);
        for (int i = 0; i < lower.length(); i += 2) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(lower, i, i + 2);
        }
        String normalized = mac.toString();
        // 与 OTA / 设备侧鉴权同一口径：只接受单播 MAC，否则固件侧后续也连不上
        return CommonUtils.isMacAddressValid(normalized) ? normalized : null;
    }

    private String requireMac(String raw) {
        String mac = normalizeMac(raw);
        if (mac == null) {
            throw StackChanAppException.business("Invalid device MAC address.");
        }
        return mac;
    }

    private String toPureMac(String normalizedMac) {
        return normalizedMac.replace(":", "");
    }

    private String formatBindTime(LocalDateTime createTime) {
        return createTime == null ? "" : BIND_TIME_FORMATTER.format(createTime);
    }
}
