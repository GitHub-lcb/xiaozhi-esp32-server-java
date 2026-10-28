package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "固件版本")
public class FirmwareResp {

    @Schema(description = "固件ID")
    private Integer firmwareId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "适用芯片型号，空表示不限")
    private String chipModel;

    @Schema(description = "固件文件地址")
    private String fileUrl;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String md5;

    @Schema(description = "发布说明")
    private String releaseNotes;

    @Schema(description = "状态(1启用 0停用)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
