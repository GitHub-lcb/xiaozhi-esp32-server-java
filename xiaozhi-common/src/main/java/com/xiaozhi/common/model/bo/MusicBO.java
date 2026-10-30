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
    private String state;
    private LocalDateTime createTime;
}
