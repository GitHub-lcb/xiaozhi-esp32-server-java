package com.xiaozhi.memorygraph.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryEntityDO;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryRelationDO;
import com.xiaozhi.memorygraph.dal.mysql.mapper.MemoryEntityMapper;
import com.xiaozhi.memorygraph.dal.mysql.mapper.MemoryRelationMapper;
import com.xiaozhi.memorygraph.service.MemoryGraphService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MemoryGraphServiceImpl implements MemoryGraphService {

    @Resource
    private MemoryEntityMapper memoryEntityMapper;

    @Resource
    private MemoryRelationMapper memoryRelationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsertEntity(Integer userId, String name, String type) {
        if (userId == null || !StringUtils.hasText(name)) {
            return null;
        }
        String trimmed = name.trim();
        MemoryEntityDO existing = memoryEntityMapper.selectOne(Wrappers.<MemoryEntityDO>lambdaQuery()
            .eq(MemoryEntityDO::getUserId, userId)
            .eq(MemoryEntityDO::getName, trimmed)
            .last("limit 1"));
        if (existing != null) {
            return existing.getEntityId();
        }
        MemoryEntityDO entity = new MemoryEntityDO();
        entity.setUserId(userId);
        entity.setName(trimmed);
        entity.setType(type);
        memoryEntityMapper.insert(entity);
        return entity.getEntityId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRelation(Integer userId, Long fromEntityId, Long toEntityId, String relation) {
        if (userId == null || fromEntityId == null || toEntityId == null || !StringUtils.hasText(relation)) {
            return;
        }
        Long existing = memoryRelationMapper.selectCount(Wrappers.<MemoryRelationDO>lambdaQuery()
            .eq(MemoryRelationDO::getUserId, userId)
            .eq(MemoryRelationDO::getFromEntityId, fromEntityId)
            .eq(MemoryRelationDO::getToEntityId, toEntityId)
            .eq(MemoryRelationDO::getRelation, relation.trim()));
        if (existing != null && existing > 0) {
            return;
        }
        MemoryRelationDO row = new MemoryRelationDO();
        row.setUserId(userId);
        row.setFromEntityId(fromEntityId);
        row.setToEntityId(toEntityId);
        row.setRelation(relation.trim());
        memoryRelationMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRelationByNames(Integer userId, String fromName, String relation, String toName,
                                   String fromType, String toType) {
        if (!StringUtils.hasText(fromName) || !StringUtils.hasText(toName) || !StringUtils.hasText(relation)) {
            return;
        }
        Long from = upsertEntity(userId, fromName, fromType);
        Long to = upsertEntity(userId, toName, toType);
        if (from != null && to != null) {
            addRelation(userId, from, to, relation);
        }
    }

    @Override
    public List<String> listRelationStatements(Integer userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        List<MemoryEntityDO> entities = memoryEntityMapper.selectList(
            Wrappers.<MemoryEntityDO>lambdaQuery().eq(MemoryEntityDO::getUserId, userId));
        Map<Long, String> names = new HashMap<>();
        for (MemoryEntityDO entity : entities) {
            names.put(entity.getEntityId(), entity.getName());
        }
        List<MemoryRelationDO> relations = memoryRelationMapper.selectList(
            Wrappers.<MemoryRelationDO>lambdaQuery()
                .eq(MemoryRelationDO::getUserId, userId)
                .orderByDesc(MemoryRelationDO::getRelationId)
                .last("limit " + Math.max(1, limit)));
        List<String> statements = new ArrayList<>(relations.size());
        for (MemoryRelationDO relation : relations) {
            String from = names.get(relation.getFromEntityId());
            String to = names.get(relation.getToEntityId());
            if (from != null && to != null) {
                statements.add(from + " " + relation.getRelation() + " " + to);
            }
        }
        return statements;
    }
}
