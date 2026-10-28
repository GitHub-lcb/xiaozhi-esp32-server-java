package com.xiaozhi.firmware;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.FirmwareBO;
import com.xiaozhi.firmware.convert.FirmwareConvert;
import com.xiaozhi.firmware.service.FirmwareService;
import com.xiaozhi.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 钉住固件接口的分页参数绑定、创建时的必填校验，以及删除接口的路径变量绑定。
 */
@ExtendWith(MockitoExtension.class)
class FirmwareControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;

    @Mock
    private FirmwareService firmwareService;

    @BeforeEach
    void setUp() {
        FirmwareController controller = new FirmwareController();
        ReflectionTestUtils.setField(controller, "firmwareService", firmwareService);
        ReflectionTestUtils.setField(controller, "firmwareConvert", Mappers.getMapper(FirmwareConvert.class));
        mockMvc = buildMockMvc(controller);
    }

    @Test
    void listReturnsPagedFirmwares() throws Exception {
        FirmwareBO firmware = new FirmwareBO();
        firmware.setFirmwareId(1);
        firmware.setVersion("1.2.3");
        firmware.setState(FirmwareBO.STATE_ENABLED);
        when(firmwareService.page(1, 10, null, null, null))
            .thenReturn(new PageResult<>(List.of(firmware), 1L, 1, 10));

        mockMvc.perform(get("/api/firmware")
                .param("pageNo", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].version").value("1.2.3"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void createRejectsBlankVersion() throws Exception {
        mockMvc.perform(post("/api/firmware")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"\",\"fileUrl\":\"https://example.com/a.bin\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("版本号不能为空"));
    }

    @Test
    void createRejectsBlankFileUrl() throws Exception {
        mockMvc.perform(post("/api/firmware")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"1.0.1\",\"fileUrl\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("固件文件地址不能为空"));
    }

    @Test
    void deleteBindsPathVariable() throws Exception {
        mockMvc.perform(delete("/api/firmware/3"))
            .andExpect(status().isOk());

        verify(firmwareService).delete(3);
    }
}
