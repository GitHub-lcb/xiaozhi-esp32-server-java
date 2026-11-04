package com.xiaozhi.operationlog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.OperationLogBO;
import com.xiaozhi.operationlog.convert.OperationLogConvert;
import com.xiaozhi.operationlog.dal.mysql.dataobject.OperationLogDO;
import com.xiaozhi.operationlog.dal.mysql.mapper.OperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 查询口径：DO → BO 的字段映射与分页元信息必须原样带回，页面才不会把页码/总数显示错。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperationLogServiceImplTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operationLogService, "operationLogConvert",
            Mappers.getMapper(OperationLogConvert.class));
    }

    @Test
    void pageMapsRecordsAndPaginationMetadata() {
        OperationLogDO dataObject = new OperationLogDO();
        dataObject.setId(9L);
        dataObject.setUserId(7);
        dataObject.setModule("设备管理");
        dataObject.setOperation("创建设备");
        dataObject.setMethod("POST");
        dataObject.setUrl("/api/device");
        dataObject.setHandler("DeviceController#create");
        dataObject.setParams("[\"aa:bb:cc:dd:ee:ff\"]");
        dataObject.setSuccess(true);
        dataObject.setCostMs(12);
        dataObject.setCreateTime(LocalDateTime.of(2026, 11, 3, 10, 0, 0));

        Page<OperationLogDO> doPage = new Page<>(2, 20);
        doPage.setRecords(List.of(dataObject));
        doPage.setTotal(41L);
        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(doPage);

        PageResult<OperationLogBO> result = operationLogService.page(2, 20, "设备", "创建", 7, true,
            LocalDateTime.of(2026, 11, 1, 0, 0, 0), LocalDateTime.of(2026, 11, 3, 23, 59, 59));

        assertThat(result.getTotal()).isEqualTo(41L);
        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getList()).hasSize(1);
        OperationLogBO bo = result.getList().get(0);
        assertThat(bo.getId()).isEqualTo(9L);
        assertThat(bo.getUserId()).isEqualTo(7);
        assertThat(bo.getModule()).isEqualTo("设备管理");
        assertThat(bo.getOperation()).isEqualTo("创建设备");
        assertThat(bo.getSuccess()).isTrue();
        assertThat(bo.getCostMs()).isEqualTo(12);
        assertThat(bo.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 0, 0));
    }

    @Test
    void pageReturnsEmptyListWhenNothingMatched() {
        Page<OperationLogDO> doPage = new Page<>(1, 10);
        doPage.setRecords(List.of());
        doPage.setTotal(0L);
        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(doPage);

        PageResult<OperationLogBO> result = operationLogService.page(1, 10, null, null, null, null, null, null);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }
}
