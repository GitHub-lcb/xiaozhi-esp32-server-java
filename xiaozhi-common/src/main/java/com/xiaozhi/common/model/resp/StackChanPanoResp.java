package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 全景图响应，字段与 Go 版后端 GET /pano 的 {@code model.Pano} 对齐。
 * <p>
 * Java 字段名用 {@code createdTime}/{@code updatedTime}，JSON 键是 Go 版的 {@code createdAt}/{@code updatedAt}。
 */
@Data
@Schema(description = "StackChan 设备全景图")
public class StackChanPanoResp {

    @Schema(description = "全景图 ID")
    private Long id;

    @Schema(description = "所属设备 MAC")
    private String mac;

    @Schema(description = "全景图地址")
    private String panoUrl;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
