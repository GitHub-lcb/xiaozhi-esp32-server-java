package com.xiaozhi.screenshot.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiaozhi.common.model.bo.DeviceScreenshotBO;
import com.xiaozhi.screenshot.dal.mysql.dataobject.DeviceScreenshotDO;
import com.xiaozhi.screenshot.dal.mysql.mapper.DeviceScreenshotMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 截图回填口径：只有拿到可用图片地址才算成功，其余一律记失败并留下原因，
 * 否则页面上会出现一条「成功但没有图」的僵尸记录。
 */
@ExtendWith(MockitoExtension.class)
class DeviceScreenshotServiceImplTest {

    @Mock
    private DeviceScreenshotMapper deviceScreenshotMapper;

    @InjectMocks
    private DeviceScreenshotServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            DeviceScreenshotDO.class);
    }

    @Test
    void httpUrlMarksSuccess() {
        givenPendingShot();

        service.completeByCommand(5L, Map.of("url", "https://cdn.example.com/a.png"), null);

        DeviceScreenshotDO updated = captureUpdate();
        assertThat(updated.getState()).isEqualTo(DeviceScreenshotBO.STATE_SUCCESS);
        assertThat(updated.getImageUrl()).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void dataUriMarksSuccess() {
        givenPendingShot();

        service.completeByCommand(5L, Map.of("image", "data:image/png;base64,AAAA"), null);

        DeviceScreenshotDO updated = captureUpdate();
        assertThat(updated.getState()).isEqualTo(DeviceScreenshotBO.STATE_SUCCESS);
    }

    @Test
    void payloadWithoutImageMarksFailure() {
        givenPendingShot();

        service.completeByCommand(5L, Map.of("status", "unsupported"), "设备不支持截图");

        DeviceScreenshotDO updated = captureUpdate();
        assertThat(updated.getState()).isEqualTo(DeviceScreenshotBO.STATE_FAILED);
        assertThat(updated.getMessage()).contains("不支持");
    }

    @Test
    void nonScreenshotCommandIsIgnored() {
        when(deviceScreenshotMapper.selectOne(any())).thenReturn(null);

        service.completeByCommand(6L, Map.of("url", "https://cdn.example.com/a.png"), null);

        verify(deviceScreenshotMapper, never()).updateById(any(DeviceScreenshotDO.class));
    }

    private void givenPendingShot() {
        DeviceScreenshotDO shot = new DeviceScreenshotDO();
        shot.setShotId(1L);
        shot.setDeviceId("aa:bb:cc:dd:ee:ff");
        shot.setCommandId(5L);
        shot.setState(DeviceScreenshotBO.STATE_PENDING);
        when(deviceScreenshotMapper.selectOne(any())).thenReturn(shot);
    }

    private DeviceScreenshotDO captureUpdate() {
        ArgumentCaptor<DeviceScreenshotDO> captor = ArgumentCaptor.forClass(DeviceScreenshotDO.class);
        verify(deviceScreenshotMapper).updateById(captor.capture());
        return captor.getValue();
    }
}
