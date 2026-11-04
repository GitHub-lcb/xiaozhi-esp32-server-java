package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 文件上传结果。
 * <p>
 * path 是 Go 版约定的「file/...」展示路径，url 是可直接访问的公开下载地址
 * （/api/stackChan/file/...，App Store 的图标/固件地址直接用它）。
 */
@Data
@Schema(description = "StackChan 文件上传结果")
public class StackChanFileUploadResp {

    @Schema(description = "文件路径，形如 file/apps/xxxx.bin", example = "file/apps/xxxx.bin")
    private String path;

    @Schema(description = "公开访问地址", example = "http://host:8091/api/stackChan/file/apps/xxxx.bin")
    private String url;
}
