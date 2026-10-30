package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 曲库中的一首歌。 */
@Data
public class MusicBO {

    private Long musicId;
    private String fileName;
    private String title;
    private String artist;
    private String aliases;
    private Integer durationSec;
    private Long sizeBytes;
    private Integer playCount;
    /** "1" 已上架、"0" 已下架；下架保留行是为了不丢点播历史 */
    private String state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
