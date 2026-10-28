package com.xiaozhi.firmware.service.impl;

import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.common.model.bo.FirmwareReleaseBO;
import com.xiaozhi.firmware.convert.FirmwareReleaseConvert;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareDO;
import com.xiaozhi.firmware.dal.mysql.dataobject.FirmwareReleaseDO;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareMapper;
import com.xiaozhi.firmware.dal.mysql.mapper.FirmwareReleaseMapper;
import com.xiaozhi.utils.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 钉住 OTA 下发决策：时间窗、白名单、灰度比例、芯片型号、版本高低，任何一条不满足都不能下发。
 * 决策错了的表现是「设备被刷成不该刷的版本」，属于不可回滚的线上事故，所以逐条锁死。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FirmwareReleaseServiceImplTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";
    private static final String CHIP = "esp32s3";

    @Mock
    private FirmwareReleaseMapper releaseMapper;

    @Mock
    private FirmwareMapper firmwareMapper;

    @InjectMocks
    private FirmwareReleaseServiceImpl releaseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(releaseService, "releaseConvert",
            Mappers.getMapper(FirmwareReleaseConvert.class));
        // 锁在 2026-10-28 10:00，时间窗断言不依赖跑测试的机器时间
        DateUtils.use(Clock.fixed(
            LocalDateTime.of(2026, 10, 28, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        DateUtils.reset();
    }

    @Test
    void noActiveReleaseMeansNoUpgrade() {
        when(releaseMapper.selectList(any())).thenReturn(List.of());

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void releaseInsideWindowAndNewerVersionIsSelected() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        Optional<FirmwareReleaseBO> result = releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0");

        assertThat(result).isPresent();
        assertThat(result.get().getReleaseId()).isEqualTo(1);
    }

    @Test
    void releaseStartingInFutureIsIgnored() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null,
            LocalDateTime.of(2026, 10, 28, 12, 0), null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void releaseAlreadyEndedIsIgnored() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null,
            LocalDateTime.of(2026, 10, 28, 8, 0), LocalDateTime.of(2026, 10, 28, 9, 0)));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void whitelistOnlyMatchesListedDevice() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_WHITELIST, null,
            null, null, "11:22:33:44:55:66"));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void whitelistMatchesCaseInsensitively() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_WHITELIST, null,
            null, null, DEVICE_ID.toUpperCase()));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isPresent();
    }

    @Test
    void percentBucketIsStablePerDevice() {
        FirmwareReleaseDO release = release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_PERCENT, 1,
            null, null);
        givenReleases(release);
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        boolean first = releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0").isPresent();
        boolean second = releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0").isPresent();

        assertThat(first).isEqualTo(second);
        assertThat(Math.floorMod(DEVICE_ID.hashCode(), 100) < 1).isEqualTo(first);
    }

    @Test
    void oneHundredPercentAlwaysMatches() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_PERCENT, 100,
            null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isPresent();
    }

    @Test
    void sameOrOlderVersionIsIgnored() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.0.0", FirmwareBO.STATE_ENABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void disabledFirmwareIsIgnored() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_DISABLED, CHIP);

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void chipModelMustMatch() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, "esp32c3");

        assertThat(releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0")).isEmpty();
    }

    @Test
    void deviceWithoutChipModelDoesNotGetChipSpecificFirmware() {
        givenReleases(release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);

        // 设备没上报芯片时，宁可不升，也不冒险刷特定芯片的包
        assertThat(releaseService.resolveForDevice(DEVICE_ID, null, "1.0.0")).isEmpty();
    }

    @Test
    void highestVersionWinsWhenSeveralMatch() {
        givenReleases(
            release(1, 10, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null),
            release(2, 20, FirmwareReleaseBO.STATE_ACTIVE, FirmwareReleaseBO.STRATEGY_ALL, null, null, null));
        givenFirmware(10, "1.1.0", FirmwareBO.STATE_ENABLED, CHIP);
        givenFirmware(20, "1.2.0", FirmwareBO.STATE_ENABLED, CHIP);

        Optional<FirmwareReleaseBO> result = releaseService.resolveForDevice(DEVICE_ID, CHIP, "1.0.0");

        assertThat(result).isPresent();
        assertThat(result.get().getFirmwareId()).isEqualTo(20);
    }

    private void givenReleases(FirmwareReleaseDO... releases) {
        when(releaseMapper.selectList(any())).thenReturn(List.of(releases));
    }

    private void givenFirmware(Integer firmwareId, String version, String state, String chipModel) {
        FirmwareDO firmware = new FirmwareDO();
        firmware.setFirmwareId(firmwareId);
        firmware.setVersion(version);
        firmware.setState(state);
        firmware.setChipModel(chipModel);
        when(firmwareMapper.selectById(firmwareId)).thenReturn(firmware);
    }

    private FirmwareReleaseDO release(Integer releaseId, Integer firmwareId, String state, String strategy,
                                      Integer percent, LocalDateTime startTime, LocalDateTime endTime) {
        return release(releaseId, firmwareId, state, strategy, percent, startTime, endTime, null);
    }

    private FirmwareReleaseDO release(Integer releaseId, Integer firmwareId, String state, String strategy,
                                      Integer percent, LocalDateTime startTime, LocalDateTime endTime,
                                      String whitelist) {
        FirmwareReleaseDO release = new FirmwareReleaseDO();
        release.setReleaseId(releaseId);
        release.setFirmwareId(firmwareId);
        release.setState(state);
        release.setStrategy(strategy);
        release.setPercent(percent);
        release.setStartTime(startTime);
        release.setEndTime(endTime);
        release.setWhitelist(whitelist);
        return release;
    }
}
