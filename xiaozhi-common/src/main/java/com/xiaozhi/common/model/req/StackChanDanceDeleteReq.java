package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "StackChan 舞蹈删除请求")
public class StackChanDanceDeleteReq {

    @Schema(description = "舞蹈 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "舞蹈ID不能为空")
    private Long id;
}
