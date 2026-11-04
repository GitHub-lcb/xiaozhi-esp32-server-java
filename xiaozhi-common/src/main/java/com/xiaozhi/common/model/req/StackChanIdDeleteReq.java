package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "StackChan 管理端按 ID 删除资源请求")
public class StackChanIdDeleteReq {

    @NotNull(message = "ID 不能为空")
    @Schema(description = "资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
