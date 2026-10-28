package com.xiaozhi.screenshot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceScreenshotBO;
import com.xiaozhi.screenshot.convert.DeviceScreenshotConvert;
import com.xiaozhi.screenshot.dal.mysql.dataobject.DeviceScreenshotDO;
import com.xiaozhi.screenshot.dal.mysql.mapper.DeviceScreenshotMapper;
import com.xiaozhi.screenshot.service.DeviceScreenshotService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeviceScreenshotServiceImpl implements DeviceScreenshotService {

    /** 设备回传里可能放图片地址的字段名，按优先级尝试 */
    private static final String[] URL_KEYS = {"url", "imageUrl", "image_url", "snapshot", "image"};

    @Resource
    private DeviceScreenshotMapper deviceScreenshotMapper;

    @Resource
    private DeviceScreenshotConvert deviceScreenshotConvert;

    @Override
    public PageResult<DeviceScreenshotBO> page(int pageNo, int pageSize, String deviceId, String state) {
        LambdaQueryWrapper<DeviceScreenshotDO> query = Wrappers.<DeviceScreenshotDO>lambdaQuery()
            .like(StringUtils.hasText(deviceId), DeviceScreenshotDO::getDeviceId, deviceId)
            .eq(StringUtils.hasText(state), DeviceScreenshotDO::getState, state)
            .orderByDesc(DeviceScreenshotDO::getShotId);
        IPage<DeviceScreenshotDO> result = deviceScreenshotMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<DeviceScreenshotBO> list = result.getRecords().stream().map(deviceScreenshotConvert::toBO).toList();
        return new PageResult<>(list, result.getTotal(),
            Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceScreenshotBO record(DeviceScreenshotBO bo) {
        DeviceScreenshotDO dataObject = deviceScreenshotConvert.toDO(bo);
        dataObject.setState(DeviceScreenshotBO.STATE_PENDING);
        deviceScreenshotMapper.insert(dataObject);
        return deviceScreenshotConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeByCommand(Long commandId, Map<String, Object> payload, String errorMessage) {
        if (commandId == null) {
            return;
        }
        DeviceScreenshotDO dataObject = deviceScreenshotMapper.selectOne(
            Wrappers.<DeviceScreenshotDO>lambdaQuery().eq(DeviceScreenshotDO::getCommandId, commandId).last("limit 1"));
        if (dataObject == null) {
            // 不是截图指令，正常情况
            return;
        }
        String url = extractUrl(payload);
        if (url != null) {
            dataObject.setState(DeviceScreenshotBO.STATE_SUCCESS);
            dataObject.setImageUrl(url);
            dataObject.setMessage(null);
        } else {
            dataObject.setState(DeviceScreenshotBO.STATE_FAILED);
            dataObject.setMessage(abbreviate(errorMessage != null ? errorMessage : JsonUtil.toJson(payload)));
        }
        deviceScreenshotMapper.updateById(dataObject);
    }

    /**
     * 从设备返回里取图片地址：优先 http(s) 地址，其次 data URI。
     * 纯 base64 不落库（体积不可控），这种情况按失败处理并提示改用 URL 回传。
     */
    private String extractUrl(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        for (String key : URL_KEYS) {
            Object value = payload.get(key);
            if (value instanceof String text && StringUtils.hasText(text)) {
                if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("data:image/")) {
                    return text;
                }
            }
        }
        return null;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
