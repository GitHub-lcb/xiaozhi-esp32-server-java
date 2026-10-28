package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "固件升级任务状态变更请求")
public class FirmwareReleaseStateReq {

    @Schema(description = "目标状态：1进行中 2已暂停 3已结束", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "^[123]$", message = "目标状态只能是 1进行中 / 2已暂停 / 3已结束")
    private String state;
}
