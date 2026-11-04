package com.xiaozhi.stackchan.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan App Center 条目（P0 先返回空列表，App Store 迁移到 Java 后填充）。
 */
@Data
@Schema(description = "StackChan App Center 条目")
public class StackChanAppResp {

    @Schema(description = "应用 ID")
    private Long id;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用图标地址")
    private String appIconUrl;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "固件/安装包下载地址")
    private String firmwareUrl;
}
