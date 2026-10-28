package com.xiaozhi.device;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.DeviceSelfCheckBO;
import com.xiaozhi.common.model.bo.SelfCheckItemBO;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.communication.auth.DeviceAuthService;
import com.xiaozhi.communication.registry.DialogueServerInfo;
import com.xiaozhi.communication.registry.DialogueServerRegistry;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.firmware.service.FirmwareReleaseService;
import com.xiaozhi.firmware.service.FirmwareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 钉住自检的结论口径：能接进来的设备不应该被判失败，地址/鉴权/通道有问题必须报出来，
 * 否则「一键自检」只会让人更迷惑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceSelfCheckServiceTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";

    @Mock
    private DeviceService deviceService;

    @Mock
    private ServerAddressProvider serverAddressProvider;

    @Mock
    private DialogueServerRegistry dialogueServerRegistry;

    @Mock
    private DeviceAuthService deviceAuthService;

    @Mock
    private FirmwareService firmwareService;

    @Mock
    private FirmwareReleaseService firmwareReleaseService;

    @Mock
    private DeviceChannelService deviceChannelService;

    @InjectMocks
    private DeviceSelfCheckService selfCheckService;

    @BeforeEach
    void setUp() {
        when(deviceService.getBO(DEVICE_ID)).thenReturn(onlineDevice());
        when(serverAddressProvider.getOtaAddress()).thenReturn("http://192.168.1.10:8091/api/device/ota");
        when(serverAddressProvider.getWebsocketAddress()).thenReturn("ws://192.168.1.10:8092/ws/xiaozhi/v1/");
        when(dialogueServerRegistry.getAvailableServers()).thenReturn(List.of(serverInfo()));
        when(deviceAuthService.isEnabled()).thenReturn(true);
        when(firmwareReleaseService.resolveForDevice(any(), any(), any())).thenReturn(Optional.empty());
        DeviceChannelBO channel = new DeviceChannelBO();
        channel.setDeviceId(DEVICE_ID);
        channel.setChannel(DeviceChannelBO.CHANNEL_MQTT);
        channel.setOnlineState(DeviceChannelBO.ONLINE);
        when(deviceChannelService.get(DEVICE_ID)).thenReturn(channel);
    }

    @Test
    void healthyDevicePassesEveryCheck() {
        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(result.getSummary()).isEqualTo(DeviceSelfCheckBO.SUMMARY_PASS);
        assertThat(result.getFailed()).isZero();
        assertThat(result.getWarned()).isZero();
        assertThat(result.getItems()).extracting(SelfCheckItemBO::getKey)
            .containsExactly("device-bound", "device-online", "device-meta", "ota-address",
                "dialogue-channel", "mqtt-channel", "device-auth", "firmware-upgrade");
    }

    @Test
    void missingDialogueInstanceIsReportedAsWarning() {
        when(dialogueServerRegistry.getAvailableServers()).thenReturn(List.of());

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(result.getSummary()).isEqualTo(DeviceSelfCheckBO.SUMMARY_WARN);
        assertThat(item(result, "dialogue-channel").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_WARN);
        assertThat(item(result, "dialogue-channel").getSuggestion()).contains("dialogue");
    }

    @Test
    void disabledDeviceAuthIsReportedAsWarning() {
        when(deviceAuthService.isEnabled()).thenReturn(false);

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(item(result, "device-auth").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_WARN);
        assertThat(item(result, "device-auth").getSuggestion()).contains("XIAOZHI_DEVICE_AUTH_SECRET");
    }

    @Test
    void loopbackOtaAddressIsReportedAsWarning() {
        when(serverAddressProvider.getOtaAddress()).thenReturn("http://127.0.0.1:8091/api/device/ota");

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(item(result, "ota-address").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_WARN);
    }

    @Test
    void unknownDeviceFailsBindingAndKeepsCheckingInfrastructure() {
        when(deviceService.getBO(DEVICE_ID)).thenReturn(null);

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(result.getSummary()).isEqualTo(DeviceSelfCheckBO.SUMMARY_FAIL);
        assertThat(item(result, "device-bound").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_FAIL);
        // 基础设施项不依赖设备，仍然要给出结论
        assertThat(item(result, "ota-address").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_PASS);
    }

    @Test
    void offlineDeviceWarnsButDoesNotFail() {
        DeviceBO device = onlineDevice();
        device.setState(DeviceBO.DEVICE_STATE_OFFLINE);
        when(deviceService.getBO(DEVICE_ID)).thenReturn(device);

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(result.getSummary()).isEqualTo(DeviceSelfCheckBO.SUMMARY_WARN);
        assertThat(item(result, "device-online").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_WARN);
    }

    @Test
    void missingChipModelIsReportedAsWarning() {
        DeviceBO device = onlineDevice();
        device.setChipModelName(null);
        when(deviceService.getBO(DEVICE_ID)).thenReturn(device);

        DeviceSelfCheckBO result = selfCheckService.check(DEVICE_ID);

        assertThat(item(result, "device-meta").getStatus()).isEqualTo(SelfCheckItemBO.STATUS_WARN);
    }

    private SelfCheckItemBO item(DeviceSelfCheckBO result, String key) {
        return result.getItems().stream()
            .filter(entry -> key.equals(entry.getKey()))
            .findFirst()
            .orElseThrow();
    }

    private DeviceBO onlineDevice() {
        DeviceBO device = new DeviceBO();
        device.setDeviceId(DEVICE_ID);
        device.setDeviceName("客厅小智");
        device.setState(DeviceBO.DEVICE_STATE_ONLINE);
        device.setChipModelName("esp32s3");
        device.setVersion("1.0.0");
        device.setUserId(1);
        device.setIp("192.168.1.20");
        device.setWifiName("home");
        return device;
    }

    private DialogueServerInfo serverInfo() {
        DialogueServerInfo info = new DialogueServerInfo();
        info.setInstanceId("dialogue-1");
        info.setWebsocketAddress("ws://192.168.1.10:8092/ws/xiaozhi/v1/");
        return info;
    }
}
