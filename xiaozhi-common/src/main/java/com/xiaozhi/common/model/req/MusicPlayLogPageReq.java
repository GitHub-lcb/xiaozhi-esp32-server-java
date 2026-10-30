package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "点播记录分页查询")
public class MusicPlayLogPageReq extends BasePageReq {

    @Schema(description = "关键字，模糊匹配点播词/歌名")
    private String keyword;

    @Schema(description = "设备ID，精确匹配")
    private String deviceId;
}
