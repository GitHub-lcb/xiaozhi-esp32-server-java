package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 一次点播记录。 */
@Data
public class MusicPlayLogBO {

    private Long logId;
    private String deviceId;
    private Long musicId;
    private String keyword;
    private String songName;
    private String finished;
    private LocalDateTime createTime;
}
