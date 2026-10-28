package com.xiaozhi.devicegroup.service;

import java.util.List;

/**
 * 设备分组（同一空间的多台设备）。唤醒路径上要频繁查「这台设备属于哪个组」，所以带本地缓存。
 */
public interface DeviceGroupService {

    Long groupIdOf(String deviceId);

    Long createGroup(Integer userId, String groupName);

    void addMember(Long groupId, String deviceId);

    void removeMember(Long groupId, String deviceId);

    List<String> listDeviceIds(Long groupId);

    /** 分组变更后清缓存 */
    void evict(String deviceId);
}
