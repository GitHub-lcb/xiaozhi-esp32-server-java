package com.xiaozhi.channel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.channel.convert.DeviceChannelConvert;
import com.xiaozhi.channel.dal.mysql.dataobject.DeviceChannelDO;
import com.xiaozhi.channel.dal.mysql.mapper.DeviceChannelMapper;
import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceChannelServiceImpl implements DeviceChannelService {

    @Resource
    private DeviceChannelMapper deviceChannelMapper;

    @Resource
    private DeviceChannelConvert deviceChannelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOnline(String deviceId, String channel, String clientId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        LocalDateTime now = DateUtils.now();
        DeviceChannelDO dataObject = deviceChannelMapper.selectById(deviceId);
        if (dataObject == null) {
            dataObject = new DeviceChannelDO();
            dataObject.setDeviceId(deviceId);
            dataObject.setChannel(channel);
            dataObject.setClientId(clientId);
            dataObject.setOnlineState(DeviceChannelBO.ONLINE);
            dataObject.setLastSeenTime(now);
            deviceChannelMapper.insert(dataObject);
            return;
        }
        dataObject.setChannel(channel);
        dataObject.setClientId(clientId);
        dataObject.setOnlineState(DeviceChannelBO.ONLINE);
        dataObject.setLastSeenTime(now);
        deviceChannelMapper.updateById(dataObject);
    }

    @Override
    public void touch(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        // 只更新存在的记录：没有任何通道登记过的设备，等它真正接入时再建行
        deviceChannelMapper.update(null, Wrappers.<DeviceChannelDO>lambdaUpdate()
            .eq(DeviceChannelDO::getDeviceId, deviceId)
            .set(DeviceChannelDO::getOnlineState, DeviceChannelBO.ONLINE)
            .set(DeviceChannelDO::getLastSeenTime, DateUtils.now()));
    }

    @Override
    public void markOffline(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        deviceChannelMapper.update(null, Wrappers.<DeviceChannelDO>lambdaUpdate()
            .eq(DeviceChannelDO::getDeviceId, deviceId)
            .set(DeviceChannelDO::getOnlineState, DeviceChannelBO.OFFLINE));
    }

    @Override
    public int sweepStale(Duration ttl) {
        LocalDateTime threshold = DateUtils.now().minus(ttl);
        int updated = deviceChannelMapper.update(null, Wrappers.<DeviceChannelDO>lambdaUpdate()
            .eq(DeviceChannelDO::getOnlineState, DeviceChannelBO.ONLINE)
            .lt(DeviceChannelDO::getLastSeenTime, threshold)
            .set(DeviceChannelDO::getOnlineState, DeviceChannelBO.OFFLINE));
        if (updated > 0) {
            log.info("通道巡检：{} 台设备超过 {} 没有消息，置为离线", updated, ttl);
        }
        return updated;
    }

    @Override
    public DeviceChannelBO get(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        DeviceChannelDO dataObject = deviceChannelMapper.selectById(deviceId);
        return dataObject == null ? null : deviceChannelConvert.toBO(dataObject);
    }

    @Override
    public Map<String, DeviceChannelBO> mapByDeviceIds(Collection<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Map.of();
        }
        List<DeviceChannelDO> rows = deviceChannelMapper.selectBatchIds(deviceIds);
        return rows.stream()
            .map(deviceChannelConvert::toBO)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(DeviceChannelBO::getDeviceId, Function.identity(), (a, b) -> a));
    }
}
