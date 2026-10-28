package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "固件版本创建请求")
public class FirmwareCreateReq {

    @Schema(description = "版本号，如 1.2.3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "版本号不能为空")
    private String version;

    @Schema(description = "适用芯片型号，空表示不限")
    private String chipModel;

    @Schema(description = "固件文件地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "固件文件地址不能为空")
    private String fileUrl;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String md5;

    @Schema(description = "发布说明")
    private String releaseNotes;

    @Schema(description = "状态(1启用 0停用)")
    private String state;
}
