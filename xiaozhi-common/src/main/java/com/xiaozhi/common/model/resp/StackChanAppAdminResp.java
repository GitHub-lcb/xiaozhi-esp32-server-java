package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 StackChan App Store 条目（含上下架状态与时间，公开接口不下发这些字段）。
 */
@Data
@Schema(description = "StackChan App Store 条目（管理端）")
public class StackChanAppAdminResp {

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

    @Schema(description = "状态：1 上架 0 下架")
    private Integer state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
