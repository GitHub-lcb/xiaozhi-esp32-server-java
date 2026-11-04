package com.xiaozhi.operationlog;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.OperationLogBO;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.operationlog.convert.OperationLogConvert;
import com.xiaozhi.operationlog.service.OperationLogService;
import com.xiaozhi.support.ControllerTestSupport;
import com.xiaozhi.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 钉住操作日志查询的契约：筛选参数绑定（含时间格式）、单页上限、用户名回填。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperationLogControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        OperationLogController controller = new OperationLogController();
        ReflectionTestUtils.setField(controller, "operationLogService", operationLogService);
        ReflectionTestUtils.setField(controller, "operationLogConvert",
            Mappers.getMapper(OperationLogConvert.class));
        ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = buildMockMvc(controller);
    }

    @Test
    void listReturnsPagedLogsWithUsername() throws Exception {
        OperationLogBO bo = new OperationLogBO();
        bo.setId(1L);
        bo.setUserId(7);
        bo.setModule("设备管理");
        bo.setOperation("创建设备");
        bo.setMethod("POST");
        bo.setUrl("/api/device");
        bo.setHandler("DeviceController#create");
        bo.setSuccess(true);
        bo.setCostMs(12);
        bo.setCreateTime(LocalDateTime.of(2026, 11, 3, 10, 0, 0));
        when(operationLogService.page(1, 10, null, null, null, null, null, null))
            .thenReturn(new PageResult<>(List.of(bo), 1L, 1, 10));

        UserBO user = new UserBO();
        user.setUserId(7);
        user.setUsername("admin");
        when(userService.getBO(7)).thenReturn(user);

        mockMvc.perform(get("/api/operation-log")
                .param("pageNo", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].module").value("设备管理"))
            .andExpect(jsonPath("$.data.list[0].username").value("admin"))
            .andExpect(jsonPath("$.data.list[0].createTime").value("2026-11-03 10:00:00"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void filtersAreBoundWithParsedTimeRange() throws Exception {
        when(operationLogService.page(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageResult<>(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/api/operation-log")
                .param("module", "设备管理")
                .param("operation", "创建")
                .param("userId", "7")
                .param("success", "true")
                .param("startTime", "2026-11-01 00:00:00")
                .param("endTime", "2026-11-03 23:59:59"))
            .andExpect(status().isOk());

        verify(operationLogService).page(eq(1), eq(10), eq("设备管理"), eq("创建"), eq(7), eq(true),
            eq(LocalDateTime.of(2026, 11, 1, 0, 0, 0)),
            eq(LocalDateTime.of(2026, 11, 3, 23, 59, 59)));
    }

    @Test
    void pageSizeIsClampedToOneHundred() throws Exception {
        when(operationLogService.page(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageResult<>(List.of(), 0L, 1, 100));

        mockMvc.perform(get("/api/operation-log").param("pageSize", "500"))
            .andExpect(status().isOk());

        verify(operationLogService).page(eq(1), eq(100), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void invalidTimeFormatIsRejected() throws Exception {
        mockMvc.perform(get("/api/operation-log").param("startTime", "2026/11/01"))
            .andExpect(status().isBadRequest());
    }
}
