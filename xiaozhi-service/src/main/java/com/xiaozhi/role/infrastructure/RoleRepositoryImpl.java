package com.xiaozhi.role.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaozhi.common.CacheHelper;
import com.xiaozhi.common.config.CacheNames;
import com.xiaozhi.event.RoleUpdatedEvent;
import com.xiaozhi.role.dal.mysql.dataobject.RoleDO;
import com.xiaozhi.role.dal.mysql.mapper.RoleMapper;
import com.xiaozhi.role.domain.Role;
import com.xiaozhi.role.domain.repository.RoleRepository;
import com.xiaozhi.role.infrastructure.convert.RoleConverter;
import com.xiaozhi.role.support.RoleCacheKeys;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Role 聚合根仓储实现。
 * <p>
 * 封装 MyBatis-Plus Mapper，负责：
 * <ul>
 *   <li>DO ↔ 聚合根转换（通过 {@link RoleConverter}）</li>
 *   <li>"唯一默认角色"不变式维护（save 时 reset 同用户其他角色）</li>
 *   <li>缓存失效</li>
 * </ul>
 * <p>
 * resetDefault 不按 state 过滤，被禁用的角色仍占着该用户的默认位。
 */
@Repository
@lombok.extern.slf4j.Slf4j
public class RoleRepositoryImpl implements RoleRepository {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleConverter roleConverter;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Role> findById(Integer roleId) {
        if (roleId == null) return Optional.empty();
        return Optional.ofNullable(roleMapper.selectById(roleId))
                .map(d -> toRole(d));
    }

    @Override
    @Transactional
    public void save(Role role) {
        RoleDO dataObject = roleConverter.toDataObject(role);

        if (role.getRoleId() == null) {
            if (role.isDefault()) {
                resetDefault(role.getUserId());
            }
            roleMapper.insert(dataObject);
            role.assignId(dataObject.getRoleId());
        } else {
            if (role.isDefault()) {
                resetDefault(role.getUserId(), role.getRoleId());
            }
            roleMapper.updateById(dataObject);
        }

        evictCache(role.getRoleId());

        var signals = role.pullSignals();
        if (signals.contains(Role.DomainSignal.UPDATED)) {
            // 换音色后是否生效，全靠这条广播：日志里必须能对上「保存了什么音色 → 发了哪次广播」
            log.info("角色保存 roleId={} 触发更新广播（信号={}）", role.getRoleId(), signals);
            eventPublisher.publishEvent(new RoleUpdatedEvent(this, role.getRoleId()));
        } else {
            log.warn("角色保存 roleId={} 未产生 UPDATED 信号，不会广播，对话侧不会重建 Persona", role.getRoleId());
        }
    }

    @Override
    @Transactional
    public void delete(Integer roleId) {
        if (roleId == null) return;
        RoleDO existing = roleMapper.selectById(roleId);
        if (existing != null) {
            roleMapper.delete(new LambdaUpdateWrapper<RoleDO>().eq(RoleDO::getRoleId, roleId));
            evictCache(roleId);
            eventPublisher.publishEvent(new RoleUpdatedEvent(this, roleId));
        }
    }

    /** 重置同用户所有角色的默认标记（insert 前调用） */
    private void resetDefault(Integer userId) {
        resetDefault(userId, null);
    }

    /** 重置同用户其他默认角色的默认标记（update 前调用，排除自身），并让被降级角色的缓存失效 */
    private void resetDefault(Integer userId, Integer excludeRoleId) {
        LambdaQueryWrapper<RoleDO> idQuery = new LambdaQueryWrapper<RoleDO>()
                .select(RoleDO::getRoleId)
                .eq(RoleDO::getUserId, userId)
                .eq(RoleDO::getIsDefault, "1");
        if (excludeRoleId != null) {
            idQuery.ne(RoleDO::getRoleId, excludeRoleId);
        }
        List<Integer> demotedRoleIds = roleMapper.selectList(idQuery).stream()
                .map(RoleDO::getRoleId)
                .toList();
        if (demotedRoleIds.isEmpty()) {
            return;
        }
        roleMapper.update(null, new LambdaUpdateWrapper<RoleDO>()
                .in(RoleDO::getRoleId, demotedRoleIds)
                .set(RoleDO::getIsDefault, "0"));
        demotedRoleIds.forEach(this::evictCache);
    }

    /** 走 evictNow：本方法在事务里跑，单调 evict 会被推迟到提交后，调用方写完回读会命中旧值 */
    private void evictCache(Integer roleId) {
        if (roleId == null) return;
        CacheHelper.evictNow(cacheManager.getCache(CacheNames.ROLE), RoleCacheKeys.of(roleId));
    }

    private Role toRole(RoleDO d) {
        return roleConverter.toDomain(d);
    }
}
