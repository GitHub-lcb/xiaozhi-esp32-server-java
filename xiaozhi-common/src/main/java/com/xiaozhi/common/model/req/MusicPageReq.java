package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "曲库分页查询")
public class MusicPageReq extends BasePageReq {

    @Schema(description = "关键字，模糊匹配歌名/歌手/别名/文件名")
    private String keyword;

    @Schema(description = "状态：1 已上架，0 已下架，空表示全部")
    private String state;

    @Schema(description = "排序字段：title/playCount/createTime/durationSec，默认 title")
    private String sortBy;

    @Schema(description = "排序方向：asc/desc，默认 asc")
    private String sortOrder;
}
