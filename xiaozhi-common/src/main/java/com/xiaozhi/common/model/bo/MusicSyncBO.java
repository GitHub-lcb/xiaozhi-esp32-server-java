package com.xiaozhi.common.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一次目录同步的结果：新增登记了多少首、因文件消失下架了多少首。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicSyncBO {

    /** 本次新登记的曲目数 */
    private Integer added;

    /** 本次因文件缺失被下架的数量 */
    private Integer removed;

    /** 同步后 sys_music 的总行数（含已下架） */
    private Long total;
}
