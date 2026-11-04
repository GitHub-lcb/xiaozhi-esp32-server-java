package com.xiaozhi.stackchan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.StackChanAppBO;
import com.xiaozhi.stackchan.dal.mysql.dataobject.StackChanAppDO;
import com.xiaozhi.stackchan.dal.mysql.mapper.StackChanAppMapper;
import com.xiaozhi.stackchan.service.StackChanAppStoreService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * StackChan App Store 实现：公开读 + 管理端增删改查，删除为软删除。
 */
@Slf4j
@Service
public class StackChanAppStoreServiceImpl implements StackChanAppStoreService {

    private static final int MAX_APP_NAME_LENGTH = 128;
    private static final int MAX_URL_LENGTH = 512;

    @Resource
    private StackChanAppMapper stackChanAppMapper;

    @Override
    public List<StackChanAppBO> listPublicApps() {
        return stackChanAppMapper.selectList(Wrappers.<StackChanAppDO>lambdaQuery()
                .eq(StackChanAppDO::getState, StackChanAppBO.STATE_ON)
                .eq(StackChanAppDO::getIsDeleted, StackChanAppBO.NOT_DELETED)
                .orderByAsc(StackChanAppDO::getAppId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public List<StackChanAppBO> listAdminApps() {
        return stackChanAppMapper.selectList(Wrappers.<StackChanAppDO>lambdaQuery()
                .eq(StackChanAppDO::getIsDeleted, StackChanAppBO.NOT_DELETED)
                .orderByDesc(StackChanAppDO::getAppId))
            .stream()
            .map(this::toBO)
            .toList();
    }

    @Override
    public StackChanAppBO get(Long appId) {
        StackChanAppDO dataObject = appId == null ? null : stackChanAppMapper.selectById(appId);
        if (dataObject == null || StackChanAppBO.DELETED.equals(dataObject.getIsDeleted())) {
            return null;
        }
        return toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanAppBO create(StackChanAppBO bo) {
        if (bo == null) {
            throw new IllegalArgumentException("应用数据不能为空");
        }
        Integer state = bo.getState() == null ? StackChanAppBO.STATE_ON : validateState(bo.getState());
        requireUrlLength(bo.getAppIconUrl(), "应用图标地址");
        requireUrlLength(bo.getFirmwareUrl(), "固件下载地址");

        StackChanAppDO dataObject = new StackChanAppDO();
        dataObject.setAppName(requireAppName(bo.getAppName()));
        dataObject.setAppIconUrl(trimToNull(bo.getAppIconUrl()));
        dataObject.setDescription(trimToNull(bo.getDescription()));
        dataObject.setFirmwareUrl(trimToNull(bo.getFirmwareUrl()));
        dataObject.setState(state);
        dataObject.setIsDeleted(StackChanAppBO.NOT_DELETED);
        stackChanAppMapper.insert(dataObject);
        log.info("StackChan 应用已创建: appId={}, appName={}", dataObject.getAppId(), dataObject.getAppName());
        return toBO(dataObject);
    }

    /**
     * 局部更新：BO 字段为 null 表示「未提供」，为空串表示「清空」。
     * <p>
     * 用 update wrapper 显式 set，而不是 updateById(实体)：MyBatis-Plus 默认忽略 null 字段，
     * 传空串清空图标/描述这类操作会静默失效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StackChanAppBO update(Long appId, StackChanAppBO bo) {
        StackChanAppDO dataObject = requireApp(appId);
        if (bo == null) {
            return toBO(dataObject);
        }
        LambdaUpdateWrapper<StackChanAppDO> update = Wrappers.<StackChanAppDO>lambdaUpdate()
            .eq(StackChanAppDO::getAppId, appId);
        boolean changed = false;
        if (StringUtils.hasText(bo.getAppName())) {
            update.set(StackChanAppDO::getAppName, requireAppName(bo.getAppName()));
            changed = true;
        }
        if (bo.getAppIconUrl() != null) {
            requireUrlLength(bo.getAppIconUrl(), "应用图标地址");
            update.set(StackChanAppDO::getAppIconUrl, trimToNull(bo.getAppIconUrl()));
            changed = true;
        }
        if (bo.getDescription() != null) {
            update.set(StackChanAppDO::getDescription, trimToNull(bo.getDescription()));
            changed = true;
        }
        if (bo.getFirmwareUrl() != null) {
            requireUrlLength(bo.getFirmwareUrl(), "固件下载地址");
            update.set(StackChanAppDO::getFirmwareUrl, trimToNull(bo.getFirmwareUrl()));
            changed = true;
        }
        if (bo.getState() != null) {
            update.set(StackChanAppDO::getState, validateState(bo.getState()));
            changed = true;
        }
        if (!changed) {
            return toBO(dataObject);
        }
        update.set(StackChanAppDO::getUpdateTime, DateUtils.now());
        stackChanAppMapper.update(null, update);
        return toBO(requireApp(appId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long appId) {
        if (appId == null) {
            throw new IllegalArgumentException("应用ID不能为空");
        }
        StackChanAppDO dataObject = stackChanAppMapper.selectById(appId);
        if (dataObject == null || StackChanAppBO.DELETED.equals(dataObject.getIsDeleted())) {
            return;
        }
        dataObject.setIsDeleted(StackChanAppBO.DELETED);
        stackChanAppMapper.updateById(dataObject);
        log.info("StackChan 应用已软删除: appId={}", appId);
    }

    private StackChanAppDO requireApp(Long appId) {
        StackChanAppDO dataObject = appId == null ? null : stackChanAppMapper.selectById(appId);
        if (dataObject == null || StackChanAppBO.DELETED.equals(dataObject.getIsDeleted())) {
            throw new ResourceNotFoundException("应用不存在");
        }
        return dataObject;
    }

    private String requireAppName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new IllegalArgumentException("应用名称不能为空");
        }
        String name = rawName.trim();
        if (name.length() > MAX_APP_NAME_LENGTH) {
            throw new IllegalArgumentException("应用名称过长，最多 " + MAX_APP_NAME_LENGTH + " 个字符");
        }
        return name;
    }

    private Integer validateState(Integer state) {
        if (!StackChanAppBO.STATE_ON.equals(state) && !StackChanAppBO.STATE_OFF.equals(state)) {
            throw new IllegalArgumentException("状态只能是 1（上架）或 0（下架）");
        }
        return state;
    }

    private void requireUrlLength(String url, String fieldName) {
        if (url != null && url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException(fieldName + "过长，最多 " + MAX_URL_LENGTH + " 个字符");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private StackChanAppBO toBO(StackChanAppDO dataObject) {
        StackChanAppBO bo = new StackChanAppBO();
        bo.setAppId(dataObject.getAppId());
        bo.setAppName(dataObject.getAppName());
        bo.setAppIconUrl(dataObject.getAppIconUrl());
        bo.setDescription(dataObject.getDescription());
        bo.setFirmwareUrl(dataObject.getFirmwareUrl());
        bo.setState(dataObject.getState());
        bo.setIsDeleted(dataObject.getIsDeleted());
        bo.setCreateTime(dataObject.getCreateTime());
        bo.setUpdateTime(dataObject.getUpdateTime());
        return bo;
    }
}
