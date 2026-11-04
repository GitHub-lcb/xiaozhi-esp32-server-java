package com.xiaozhi.stackchan.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.model.bo.StackChanFriendBO;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanFriendDO;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanFriendMapper;
import com.xiaozhi.stackchan.service.StackChanFriendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * StackChan 设备好友关系实现。
 * <p>
 * 与 Go 版后端一致：记录按发起方向单条落库，「是否已存在」双向判断，
 * 因此 A 加 B 成功后 B 再加 A 直接幂等返回，不会产生第二条反向记录。
 */
@Slf4j
@Service
public class StackChanFriendServiceImpl implements StackChanFriendService {

    /** 归一化前的 MAC：12 位十六进制，忽略分隔符 */
    private static final Pattern MAC_HEX = Pattern.compile("^[0-9A-Fa-f]{12}$");

    @Resource
    private StackChanFriendMapper stackChanFriendMapper;

    @Resource
    private DeviceService deviceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(String mac, String friendMac) {
        String macA = normalizeMac(mac);
        String macB = normalizeMac(friendMac);
        if (macA.equals(macB)) {
            throw new IllegalArgumentException("You cannot add yourself as a friend");
        }
        if (deviceService.getBO(macB) == null) {
            throw new IllegalArgumentException(
                "The friend device does not exist or the Mac address is incorrect");
        }
        if (exists(macA, macB)) {
            return;
        }
        StackChanFriendDO dataObject = new StackChanFriendDO();
        dataObject.setMacA(macA);
        dataObject.setMacB(macB);
        stackChanFriendMapper.insert(dataObject);
        log.info("StackChan 好友已添加: deviceId={}, friendDeviceId={}", macA, macB);
    }

    @Override
    public boolean exists(String macA, String macB) {
        String normalizedA = normalizeMac(macA);
        String normalizedB = normalizeMac(macB);
        return count(normalizedA, normalizedB) > 0 || count(normalizedB, normalizedA) > 0;
    }

    @Override
    public List<StackChanFriendBO> listAll() {
        return stackChanFriendMapper.selectList(Wrappers.<StackChanFriendDO>lambdaQuery()
                .orderByDesc(StackChanFriendDO::getId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id != null) {
            stackChanFriendMapper.deleteById(id);
        }
    }

    private StackChanFriendBO toBO(StackChanFriendDO dataObject) {
        StackChanFriendBO bo = new StackChanFriendBO();
        bo.setId(dataObject.getId());
        bo.setMacA(dataObject.getMacA());
        bo.setMacB(dataObject.getMacB());
        bo.setCreateTime(dataObject.getCreateTime());
        bo.setUpdateTime(dataObject.getUpdateTime());
        return bo;
    }

    private long count(String macA, String macB) {
        Long count = stackChanFriendMapper.selectCount(Wrappers.<StackChanFriendDO>lambdaQuery()
            .eq(StackChanFriendDO::getMacA, macA)
            .eq(StackChanFriendDO::getMacB, macB));
        return count == null ? 0L : count;
    }

    /**
     * 统一成小写冒号格式：设备侧 Header（AA-BB-…/AA:BB:…）与 App 侧 12 位纯十六进制都收敛到这里。
     */
    private String normalizeMac(String rawMac) {
        if (!StringUtils.hasText(rawMac)) {
            throw new IllegalArgumentException("Device MAC cannot be empty");
        }
        String hex = rawMac.trim().replaceAll("[^0-9A-Fa-f]", "");
        if (!MAC_HEX.matcher(hex).matches()) {
            throw new IllegalArgumentException("Invalid device MAC: " + rawMac);
        }
        hex = hex.toLowerCase(Locale.ROOT);
        StringBuilder mac = new StringBuilder(17);
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(hex, i, i + 2);
        }
        return mac.toString();
    }
}
