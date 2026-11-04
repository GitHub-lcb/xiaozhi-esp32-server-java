package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "StackChan App Store 应用更新请求")
public class StackChanAppUpdateReq {

    @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "应用ID不能为空")
    private Long id;

    @Schema(description = "应用名称，不传表示不修改")
    private String appName;

    @Schema(description = "应用图标地址，传空串表示清空")
    private String appIconUrl;

    @Schema(description = "应用描述，传空串表示清空")
    private String description;

    @Schema(description = "固件/安装包下载地址，传空串表示清空")
    private String firmwareUrl;

    @Schema(description = "状态：1 上架 0 下架")
    private Integer state;
}
