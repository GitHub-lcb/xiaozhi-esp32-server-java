package com.xiaozhi.device;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceSelfCheckBO;
import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.common.model.bo.FirmwareReleaseBO;
import com.xiaozhi.common.model.bo.SelfCheckItemBO;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.communication.auth.DeviceAuthService;
import com.xiaozhi.communication.registry.DialogueServerInfo;
import com.xiaozhi.communication.registry.DialogueServerRegistry;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.firmware.service.FirmwareReleaseService;
import com.xiaozhi.firmware.service.FirmwareService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.xiaozhi.common.model.bo.SelfCheckItemBO.STATUS_FAIL;
import static com.xiaozhi.common.model.bo.SelfCheckItemBO.STATUS_PASS;
import static com.xiaozhi.common.model.bo.SelfCheckItemBO.STATUS_WARN;

/**
 * 设备接入一键自检。
 * <p>
 * 设备接不进来时通常要在「设备有没有绑定 / OTA 地址设备能不能访问 / dialogue 在线实例有没有注册 /
 * 鉴权密钥配没配 / 固件版本能不能匹配」这几处来回猜。这里把这些判断集中成一次调用，
 * 每项都给判定依据（实际取到的值）和下一步该改什么。
 * <p>
 * 全部是只读检查，不发任何下行指令，不会影响设备当前状态。
 */
@Slf4j
@Service
public class DeviceSelfCheckService {

    private static final String LOCAL_ADDRESS_PREFIX = "localhost";
    private static final String LOOPBACK = "127.0.0.1";

    @Resource
    private DeviceService deviceService;

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @Resource
    private DialogueServerRegistry dialogueServerRegistry;

    @Resource
    private DeviceAuthService deviceAuthService;

    @Resource
    private FirmwareService firmwareService;

    @Resource
    private FirmwareReleaseService firmwareReleaseService;

    @Resource
    private DeviceChannelService deviceChannelService;

    public DeviceSelfCheckBO check(String deviceId) {
        List<SelfCheckItemBO> items = new ArrayList<>();
        DeviceBO device = deviceService.getBO(deviceId);

        items.add(checkBinding(deviceId, device));
        items.add(checkOnline(device));
        items.add(checkMetadata(device));
        items.add(checkOtaAddress());
        items.add(checkDialogueChannel());
        items.add(checkMqttChannel(deviceId));
        items.add(checkDeviceAuth());
        items.add(checkFirmware(deviceId, device));

        return summarize(deviceId, device, items);
    }

    private SelfCheckItemBO checkBinding(String deviceId, DeviceBO device) {
        if (device == null) {
            return SelfCheckItemBO.of("device-bound", "设备绑定", SelfCheckItemBO.STATUS_FAIL,
                "库里没有设备 " + deviceId,
                "先在「设备管理」里添加该设备，或让设备重新走一次 OTA 激活");
        }
        return SelfCheckItemBO.of("device-bound", "设备绑定", STATUS_PASS,
            "已绑定，所属用户 " + device.getUserId(), null);
    }

    private SelfCheckItemBO checkOnline(DeviceBO device) {
        if (device == null) {
            return SelfCheckItemBO.of("device-online", "设备在线状态", STATUS_WARN,
                "设备不存在，无法判断在线状态", "先完成设备绑定");
        }
        String state = device.getState();
        String lastSeen = device.getUpdateTime() == null ? "-" : device.getUpdateTime().toString();
        String detail = "状态=" + stateText(state)
            + "，IP=" + nullSafe(device.getIp())
            + "，WiFi=" + nullSafe(device.getWifiName())
            + "，最近更新=" + lastSeen;
        if (DeviceBO.DEVICE_STATE_ONLINE.equals(state)) {
            return SelfCheckItemBO.of("device-online", "设备在线状态", STATUS_PASS, detail, null);
        }
        return SelfCheckItemBO.of("device-online", "设备在线状态", STATUS_WARN, detail,
            "设备离线不影响 OTA，但语音功能需要设备连上 WebSocket；确认设备已上电、WiFi 正常、WS 地址可达");
    }

    private SelfCheckItemBO checkMetadata(DeviceBO device) {
        if (device == null) {
            return SelfCheckItemBO.of("device-meta", "设备信息完整度", STATUS_WARN,
                "设备不存在", "先完成设备绑定");
        }
        boolean hasChip = StringUtils.hasText(device.getChipModelName());
        boolean hasVersion = StringUtils.hasText(device.getVersion());
        String detail = "芯片型号=" + nullSafe(device.getChipModelName())
            + "，固件版本=" + nullSafe(device.getVersion())
            + "，设备类型=" + nullSafe(device.getType());
        if (hasChip && hasVersion) {
            return SelfCheckItemBO.of("device-meta", "设备信息完整度", STATUS_PASS, detail, null);
        }
        return SelfCheckItemBO.of("device-meta", "设备信息完整度", STATUS_WARN, detail,
            "设备没有上报芯片型号或固件版本：" + (hasChip ? "" : "限定芯片型号的升级任务不会下发；")
                + (hasVersion ? "" : "无法判断版本高低，可能反复下发同一个包"));
    }

    private SelfCheckItemBO checkOtaAddress() {
        String otaAddress = serverAddressProvider.getOtaAddress();
        if (!StringUtils.hasText(otaAddress)) {
            return SelfCheckItemBO.of("ota-address", "OTA 地址", STATUS_FAIL,
                "未取到 OTA 地址", "检查 xiaozhi.server.address / 端口配置，或查看启动日志里的「OTA服务地址」");
        }
        String lower = otaAddress.toLowerCase(Locale.ROOT);
        boolean unreachableFromDevice = lower.contains(LOCAL_ADDRESS_PREFIX) || lower.contains(LOOPBACK);
        if (unreachableFromDevice) {
            return SelfCheckItemBO.of("ota-address", "OTA 地址", STATUS_WARN,
                otaAddress,
                "地址是本机回环，设备访问不到；把 xiaozhi.server.address 改成设备能访问的内网 IP 或域名");
        }
        return SelfCheckItemBO.of("ota-address", "OTA 地址", STATUS_PASS, otaAddress, null);
    }

    private SelfCheckItemBO checkDialogueChannel() {
        List<DialogueServerInfo> servers = dialogueServerRegistry.getAvailableServers();
        String fallback = serverAddressProvider.getWebsocketAddress();
        if (servers == null || servers.isEmpty()) {
            return SelfCheckItemBO.of("dialogue-channel", "实时通道", STATUS_WARN,
                "注册中心没有可用的 dialogue 实例，回退地址 " + nullSafe(fallback),
                "启动 dialogue 进程（默认 8092）并确认它能连上同一个 Redis；多实例部署时检查心跳与 TTL");
        }
        String names = servers.stream()
            .map(server -> nullSafe(server.getInstanceId()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("-");
        return SelfCheckItemBO.of("dialogue-channel", "实时通道", STATUS_PASS,
            "可用实例 " + servers.size() + " 个：" + names, null);
    }

    /**
     * MQTT 通道是主动唤醒与主动推送的前提：没有它，服务端只能在设备主动连上来时说话。
     */
    private SelfCheckItemBO checkMqttChannel(String deviceId) {
        DeviceChannelBO channel = deviceChannelService.get(deviceId);
        if (channel == null) {
            return SelfCheckItemBO.of("mqtt-channel", "MQTT 通道", STATUS_WARN,
                "该设备从未通过 MQTT 接入",
                "需要主动唤醒/主动推送时开启 xiaozhi.mqtt.enabled 并让固件走 OTA 拿到 mqtt 配置");
        }
        String lastSeen = channel.getLastSeenTime() == null ? "-" : channel.getLastSeenTime().toString();
        String detail = "通道=" + channel.getChannel() + "，在线=" + (channel.isOnline() ? "是" : "否")
            + "，最近消息=" + lastSeen;
        if (channel.isOnline()) {
            return SelfCheckItemBO.of("mqtt-channel", "MQTT 通道", STATUS_PASS, detail, null);
        }
        return SelfCheckItemBO.of("mqtt-channel", "MQTT 通道", STATUS_WARN, detail,
            "设备当前只能被动连接；确认 broker、账号密码与主题模板，设备重启后会重新建立长连接");
    }

    private SelfCheckItemBO checkDeviceAuth() {
        if (deviceAuthService.isEnabled()) {
            return SelfCheckItemBO.of("device-auth", "设备鉴权", STATUS_PASS,
                "已开启，握手会校验签名 token", null);
        }
        return SelfCheckItemBO.of("device-auth", "设备鉴权", STATUS_WARN,
            "未开启，任何知道地址的人都能伪装设备接入",
            "配置 XIAOZHI_DEVICE_AUTH_SECRET（server 与 dialogue 必须同值）后重启两个进程");
    }

    private SelfCheckItemBO checkFirmware(String deviceId, DeviceBO device) {
        if (device == null) {
            return SelfCheckItemBO.of("firmware-upgrade", "固件版本", STATUS_WARN,
                "设备不存在，无法判断可升级版本", "先完成设备绑定");
        }
        Optional<FirmwareReleaseBO> release = firmwareReleaseService
            .resolveForDevice(deviceId, device.getChipModelName(), device.getVersion());
        if (release.isEmpty()) {
            return SelfCheckItemBO.of("firmware-upgrade", "固件版本", STATUS_PASS,
                "当前版本 " + nullSafe(device.getVersion()) + "，没有匹配到可下发的升级任务", null);
        }
        FirmwareBO target = firmwareService.getBO(release.get().getFirmwareId());
        String targetVersion = target == null ? "-" : target.getVersion();
        return SelfCheckItemBO.of("firmware-upgrade", "固件版本", STATUS_PASS,
            "可升级：" + nullSafe(device.getVersion()) + " → " + targetVersion
                + "（任务 " + nullSafe(release.get().getReleaseName()) + "）", null);
    }

    private DeviceSelfCheckBO summarize(String deviceId, DeviceBO device, List<SelfCheckItemBO> items) {
        int passed = 0;
        int warned = 0;
        int failed = 0;
        for (SelfCheckItemBO item : items) {
            if (SelfCheckItemBO.STATUS_FAIL.equals(item.getStatus())) {
                failed++;
            } else if (SelfCheckItemBO.STATUS_WARN.equals(item.getStatus())) {
                warned++;
            } else {
                passed++;
            }
        }
        DeviceSelfCheckBO result = new DeviceSelfCheckBO();
        result.setDeviceId(deviceId);
        result.setDeviceName(device == null ? null : device.getDeviceName());
        result.setSummary(failed > 0 ? DeviceSelfCheckBO.SUMMARY_FAIL
            : warned > 0 ? DeviceSelfCheckBO.SUMMARY_WARN : DeviceSelfCheckBO.SUMMARY_PASS);
        result.setPassed(passed);
        result.setWarned(warned);
        result.setFailed(failed);
        result.setCheckTime(DateUtils.now());
        result.setItems(items);
        log.info("设备接入自检完成, deviceId={}, summary={}, pass={}, warn={}, fail={}",
            deviceId, result.getSummary(), passed, warned, failed);
        return result;
    }

    private String stateText(String state) {
        if (DeviceBO.DEVICE_STATE_ONLINE.equals(state)) {
            return "在线";
        }
        if (DeviceBO.DEVICE_STATE_STANDBY.equals(state)) {
            return "待机";
        }
        return "离线";
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
