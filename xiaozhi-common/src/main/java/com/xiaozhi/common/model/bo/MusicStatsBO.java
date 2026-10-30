package com.xiaozhi.common.model.bo;

import lombok.Data;

/** 曲库统计，列表页顶部的统计卡都取这里。 */
@Data
public class MusicStatsBO {

    /** 曲目总数（含已下架） */
    private Long total;

    /** 已上架数 */
    private Long enabled;

    /** 已下架数 */
    private Long disabled;

    /** 累计点播次数 */
    private Long totalPlayCount;

    /** 累计时长（秒） */
    private Long totalDurationSec;

    /** 累计占用字节数 */
    private Long totalSizeBytes;

    /** 今日点播次数 */
    private Long todayPlayCount;
}
