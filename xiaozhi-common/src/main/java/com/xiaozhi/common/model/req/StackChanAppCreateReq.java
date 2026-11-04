package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "StackChan App Store 应用创建请求")
public class StackChanAppCreateReq {

    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @Schema(description = "应用图标地址，建议填 /api/stackChan/file/... 公开下载地址")
    private String appIconUrl;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "固件/安装包下载地址，建议填 /api/stackChan/file/... 公开下载地址")
    private String firmwareUrl;

    @Schema(description = "状态：1 上架（默认） 0 下架")
    private Integer state;
}
