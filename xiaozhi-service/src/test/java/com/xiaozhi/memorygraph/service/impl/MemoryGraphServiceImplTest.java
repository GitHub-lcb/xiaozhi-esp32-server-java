package com.xiaozhi.memorygraph.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryEntityDO;
import com.xiaozhi.memorygraph.dal.mysql.dataobject.MemoryRelationDO;
import com.xiaozhi.memorygraph.dal.mysql.mapper.MemoryEntityMapper;
import com.xiaozhi.memorygraph.dal.mysql.mapper.MemoryRelationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图谱的两条底线：同名实体不重复建、同三元组不重复加；关系语句要从实体表翻译成人话。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoryGraphServiceImplTest {

    @Mock
    private MemoryEntityMapper memoryEntityMapper;

    @Mock
    private MemoryRelationMapper memoryRelationMapper;

    @InjectMocks
    private MemoryGraphServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MemoryEntityDO.class);
        TableInfoHelper.initTableInfo(assistant, MemoryRelationDO.class);
    }

    @Test
    void existingEntityIsReused() {
        MemoryEntityDO entity = new MemoryEntityDO();
        entity.setEntityId(9L);
        entity.setName("妈妈");
        when(memoryEntityMapper.selectOne(any())).thenReturn(entity);

        assertThat(service.upsertEntity(1, "妈妈", "person")).isEqualTo(9L);
        verify(memoryEntityMapper, never()).insert(any(MemoryEntityDO.class));
    }

    @Test
    void duplicateRelationIsSkipped() {
        when(memoryRelationMapper.selectCount(any())).thenReturn(1L);

        service.addRelation(1, 2L, 3L, "喜欢");

        verify(memoryRelationMapper, never()).insert(any(MemoryRelationDO.class));
    }

    @Test
    void statementsAreRenderedWithEntityNames() {
        MemoryEntityDO mom = new MemoryEntityDO();
        mom.setEntityId(2L);
        mom.setName("妈妈");
        MemoryEntityDO tea = new MemoryEntityDO();
        tea.setEntityId(3L);
        tea.setName("乌龙茶");
        when(memoryEntityMapper.selectList(any())).thenReturn(List.of(mom, tea));
        MemoryRelationDO relation = new MemoryRelationDO();
        relation.setFromEntityId(2L);
        relation.setToEntityId(3L);
        relation.setRelation("喜欢");
        when(memoryRelationMapper.selectList(any())).thenReturn(List.of(relation));

        assertThat(service.listRelationStatements(1, 10)).containsExactly("妈妈 喜欢 乌龙茶");
    }
}
