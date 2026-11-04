package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "StackChan 管理端新增全景图请求")
public class StackChanPanoAdminCreateReq {

    @NotBlank(message = "设备 MAC 不能为空")
    @Schema(description = "设备 MAC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mac;

    @NotBlank(message = "全景图地址不能为空")
    @Schema(description = "全景图 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;
}
