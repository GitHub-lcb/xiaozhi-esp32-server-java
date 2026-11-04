package com.xiaozhi.stackchan.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.model.bo.StackChanPanoBO;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanPanoDO;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanPanoMapper;
import com.xiaozhi.stackchan.service.StackChanPanoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * StackChan 设备全景图实现：mac 一律归一化成小写冒号格式，避免同一台设备两份全景图。
 */
@Slf4j
@Service
public class StackChanPanoServiceImpl implements StackChanPanoService {

    /** 归一化前的 MAC：12 位十六进制，忽略分隔符 */
    private static final Pattern MAC_HEX = Pattern.compile("^[0-9A-Fa-f]{12}$");

    private static final int MAX_PANO_URL_LENGTH = 512;

    @Resource
    private StackChanPanoMapper stackChanPanoMapper;

    @Override
    public List<StackChanPanoBO> listByMac(String mac) {
        String normalizedMac = normalizeMac(mac);
        return stackChanPanoMapper.selectList(Wrappers.<StackChanPanoDO>lambdaQuery()
                .eq(StackChanPanoDO::getMac, normalizedMac)
                .orderByAsc(StackChanPanoDO::getId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public List<StackChanPanoBO> listAll() {
        return stackChanPanoMapper.selectList(Wrappers.<StackChanPanoDO>lambdaQuery()
                .orderByDesc(StackChanPanoDO::getId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long panoId) {
        if (panoId != null) {
            stackChanPanoMapper.deleteById(panoId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanPanoBO create(String mac, String panoUrl) {
        if (!StringUtils.hasText(panoUrl)) {
            throw new IllegalArgumentException("url cannot be empty");
        }
        String url = panoUrl.trim();
        if (url.length() > MAX_PANO_URL_LENGTH) {
            throw new IllegalArgumentException("url is too long (max " + MAX_PANO_URL_LENGTH + " characters)");
        }
        StackChanPanoDO dataObject = new StackChanPanoDO();
        dataObject.setMac(normalizeMac(mac));
        dataObject.setPanoUrl(url);
        stackChanPanoMapper.insert(dataObject);
        log.info("StackChan 全景图已创建: deviceId={}, panoId={}", dataObject.getMac(), dataObject.getId());
        return toBO(dataObject);
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

    private StackChanPanoBO toBO(StackChanPanoDO dataObject) {
        StackChanPanoBO bo = new StackChanPanoBO();
        bo.setId(dataObject.getId());
        bo.setMac(dataObject.getMac());
        bo.setPanoUrl(dataObject.getPanoUrl());
        bo.setCreateTime(dataObject.getCreateTime());
        bo.setUpdateTime(dataObject.getUpdateTime());
        return bo;
    }
}
