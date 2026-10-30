package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "曲库目录同步结果")
public class MusicSyncResp {

    @Schema(description = "本次新登记的曲目数")
    private Integer added;

    @Schema(description = "本次因文件缺失下架的数量")
    private Integer removed;

    @Schema(description = "同步后曲目总数（含已下架）")
    private Long total;
}
