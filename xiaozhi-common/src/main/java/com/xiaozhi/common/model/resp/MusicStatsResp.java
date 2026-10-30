package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "曲库统计")
public class MusicStatsResp {

    @Schema(description = "曲目总数（含已下架）")
    private Long total;

    @Schema(description = "已上架数")
    private Long enabled;

    @Schema(description = "已下架数")
    private Long disabled;

    @Schema(description = "累计点播次数")
    private Long totalPlayCount;

    @Schema(description = "累计时长(秒)")
    private Long totalDurationSec;

    @Schema(description = "累计占用(字节)")
    private Long totalSizeBytes;

    @Schema(description = "今日点播次数")
    private Long todayPlayCount;
}
