package com.xiaozhi.stackchan.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.StackChanDanceBO;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanDanceDO;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanDanceMapper;
import com.xiaozhi.stackchan.service.StackChanDanceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * StackChan 设备舞蹈实现。
 * <p>
 * mac 一律先归一化成「小写 + 冒号」再读写：设备侧 header 带的可能是大写或短横线，
 * App 侧传的是 12 位纯十六进制，不归一化会出现同一台设备两份舞蹈。
 */
@Slf4j
@Service
public class StackChanDanceServiceImpl implements StackChanDanceService {

    /** 归一化前的 MAC：12 位十六进制，忽略分隔符 */
    private static final Pattern MAC_HEX = Pattern.compile("^[0-9A-Fa-f]{12}$");

    private static final int MAX_DANCE_NAME_LENGTH = 128;
    private static final int MAX_MUSIC_URL_LENGTH = 512;

    @Resource
    private StackChanDanceMapper stackChanDanceMapper;

    @Override
    public List<StackChanDanceBO> listByMac(String mac) {
        String normalizedMac = normalizeMac(mac);
        return stackChanDanceMapper.selectList(Wrappers.<StackChanDanceDO>lambdaQuery()
                .eq(StackChanDanceDO::getMac, normalizedMac)
                .orderByAsc(StackChanDanceDO::getDanceId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public List<StackChanDanceBO> listAll(String mac) {
        return stackChanDanceMapper.selectList(Wrappers.<StackChanDanceDO>lambdaQuery()
                .eq(StringUtils.hasText(mac), StackChanDanceDO::getMac,
                    StringUtils.hasText(mac) ? normalizeMac(mac) : null)
                .orderByDesc(StackChanDanceDO::getDanceId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public StackChanDanceBO get(Long danceId) {
        StackChanDanceDO dataObject = danceId == null ? null : stackChanDanceMapper.selectById(danceId);
        return dataObject == null ? null : toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanDanceBO create(StackChanDanceBO bo) {
        if (bo == null) {
            throw new IllegalArgumentException("舞蹈数据不能为空");
        }
        String mac = normalizeMac(bo.getMac());
        String danceName = requireDanceName(bo.getDanceName());
        requireDanceData(bo.getDanceData());
        requireMusicUrlLength(bo.getMusicUrl());

        Long duplicate = stackChanDanceMapper.selectCount(Wrappers.<StackChanDanceDO>lambdaQuery()
            .eq(StackChanDanceDO::getMac, mac)
            .eq(StackChanDanceDO::getDanceName, danceName));
        if (duplicate != null && duplicate > 0) {
            throw new IllegalArgumentException("该设备下已存在同名舞蹈: " + danceName);
        }

        StackChanDanceDO dataObject = new StackChanDanceDO();
        dataObject.setMac(mac);
        dataObject.setDanceName(danceName);
        dataObject.setDanceData(bo.getDanceData().trim());
        dataObject.setMusicUrl(StringUtils.hasText(bo.getMusicUrl()) ? bo.getMusicUrl().trim() : null);
        stackChanDanceMapper.insert(dataObject);
        log.info("StackChan 舞蹈已创建: deviceId={}, danceId={}, danceName={}", mac, dataObject.getDanceId(), danceName);
        return toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanDanceBO update(Long danceId, StackChanDanceBO bo) {
        StackChanDanceDO dataObject = requireDance(danceId);
        if (bo == null) {
            return toBO(dataObject);
        }
        if (StringUtils.hasText(bo.getDanceName())) {
            dataObject.setDanceName(requireDanceName(bo.getDanceName()));
        }
        if (StringUtils.hasText(bo.getDanceData())) {
            requireDanceData(bo.getDanceData());
            dataObject.setDanceData(bo.getDanceData().trim());
        }
        if (StringUtils.hasText(bo.getMusicUrl())) {
            requireMusicUrlLength(bo.getMusicUrl());
            dataObject.setMusicUrl(bo.getMusicUrl().trim());
        }
        stackChanDanceMapper.updateById(dataObject);
        return toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long danceId) {
        if (danceId == null) {
            throw new IllegalArgumentException("舞蹈ID不能为空");
        }
        stackChanDanceMapper.deleteById(danceId);
    }

    private StackChanDanceDO requireDance(Long danceId) {
        StackChanDanceDO dataObject = danceId == null ? null : stackChanDanceMapper.selectById(danceId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("舞蹈不存在");
        }
        return dataObject;
    }

    private String requireDanceName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new IllegalArgumentException("舞蹈名称不能为空");
        }
        String name = rawName.trim();
        if (name.length() > MAX_DANCE_NAME_LENGTH) {
            throw new IllegalArgumentException("舞蹈名称过长，最多 " + MAX_DANCE_NAME_LENGTH + " 个字符");
        }
        return name;
    }

    /** 结构合法性由控制器按 JSON 数组校验，这里只挡住空数据 */
    private void requireDanceData(String danceData) {
        if (!StringUtils.hasText(danceData)) {
            throw new IllegalArgumentException("舞蹈动作数据不能为空");
        }
    }

    private void requireMusicUrlLength(String musicUrl) {
        if (musicUrl != null && musicUrl.length() > MAX_MUSIC_URL_LENGTH) {
            throw new IllegalArgumentException("音乐地址过长，最多 " + MAX_MUSIC_URL_LENGTH + " 个字符");
        }
    }

    /**
     * 统一成小写冒号格式：设备侧 Header（AA-BB-…/AA:BB:…）与 App 侧 12 位纯十六进制都收敛到这里。
     */
    private String normalizeMac(String rawMac) {
        if (!StringUtils.hasText(rawMac)) {
            throw new IllegalArgumentException("设备MAC不能为空");
        }
        String hex = rawMac.trim().replaceAll("[^0-9A-Fa-f]", "");
        if (!MAC_HEX.matcher(hex).matches()) {
            throw new IllegalArgumentException("设备MAC不合法: " + rawMac);
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

    private StackChanDanceBO toBO(StackChanDanceDO dataObject) {
        StackChanDanceBO bo = new StackChanDanceBO();
        bo.setDanceId(dataObject.getDanceId());
        bo.setMac(dataObject.getMac());
        bo.setDanceName(dataObject.getDanceName());
        bo.setDanceData(dataObject.getDanceData());
        bo.setMusicUrl(dataObject.getMusicUrl());
        bo.setCreateTime(dataObject.getCreateTime());
        bo.setUpdateTime(dataObject.getUpdateTime());
        return bo;
    }
}
