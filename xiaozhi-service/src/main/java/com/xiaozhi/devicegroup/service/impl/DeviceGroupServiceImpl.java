package com.xiaozhi.devicegroup.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.devicegroup.dal.mysql.dataobject.DeviceGroupDO;
import com.xiaozhi.devicegroup.dal.mysql.dataobject.DeviceGroupMemberDO;
import com.xiaozhi.devicegroup.dal.mysql.mapper.DeviceGroupMapper;
import com.xiaozhi.devicegroup.dal.mysql.mapper.DeviceGroupMemberMapper;
import com.xiaozhi.devicegroup.service.DeviceGroupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DeviceGroupServiceImpl implements DeviceGroupService {

    /** 设备→分组的本地缓存，避免每次唤醒都查库；分组变更时显式清理 */
    private final Map<String, Long> groupCache = new ConcurrentHashMap<>();

    @Resource
    private DeviceGroupMapper deviceGroupMapper;

    @Resource
    private DeviceGroupMemberMapper deviceGroupMemberMapper;

    @Override
    public Long groupIdOf(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        // 用 -1 作为「不在任何组」的缓存占位，避免每轮唤醒都回查库
        Long cached = groupCache.computeIfAbsent(deviceId, key -> {
            DeviceGroupMemberDO member = deviceGroupMemberMapper.selectOne(
                Wrappers.<DeviceGroupMemberDO>lambdaQuery()
                    .eq(DeviceGroupMemberDO::getDeviceId, key)
                    .orderByAsc(DeviceGroupMemberDO::getId)
                    .last("limit 1"));
            return member == null ? -1L : member.getGroupId();
        });
        return cached < 0 ? null : cached;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(Integer userId, String groupName) {
        DeviceGroupDO group = new DeviceGroupDO();
        group.setUserId(userId);
        group.setGroupName(groupName);
        deviceGroupMapper.insert(group);
        return group.getGroupId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long groupId, String deviceId) {
        if (groupId == null || !StringUtils.hasText(deviceId)) {
            return;
        }
        Long exists = deviceGroupMemberMapper.selectCount(Wrappers.<DeviceGroupMemberDO>lambdaQuery()
            .eq(DeviceGroupMemberDO::getGroupId, groupId)
            .eq(DeviceGroupMemberDO::getDeviceId, deviceId));
        if (exists != null && exists > 0) {
            return;
        }
        DeviceGroupMemberDO member = new DeviceGroupMemberDO();
        member.setGroupId(groupId);
        member.setDeviceId(deviceId);
        deviceGroupMemberMapper.insert(member);
        evict(deviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, String deviceId) {
        deviceGroupMemberMapper.delete(Wrappers.<DeviceGroupMemberDO>lambdaQuery()
            .eq(DeviceGroupMemberDO::getGroupId, groupId)
            .eq(DeviceGroupMemberDO::getDeviceId, deviceId));
        evict(deviceId);
    }

    @Override
    public List<String> listDeviceIds(Long groupId) {
        if (groupId == null) {
            return List.of();
        }
        return deviceGroupMemberMapper.selectList(Wrappers.<DeviceGroupMemberDO>lambdaQuery()
                .eq(DeviceGroupMemberDO::getGroupId, groupId))
            .stream()
            .map(DeviceGroupMemberDO::getDeviceId)
            .toList();
    }

    @Override
    public void evict(String deviceId) {
        if (StringUtils.hasText(deviceId)) {
            groupCache.remove(deviceId);
        }
    }
}
