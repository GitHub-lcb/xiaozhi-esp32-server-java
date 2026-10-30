package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 一次点播记录。 */
@Data
public class MusicPlayLogBO {

    private Long logId;
    private String deviceId;
    /** 设备名，展示用；不在点播表里冗余存，查的时候现补，设备改名后历史记录跟着变 */
    private String deviceName;
    private Long musicId;
    /** 歌名，展示用；同理由 sys_music 现查，歌名被编辑过后历史记录显示新名字 */
    private String musicTitle;
    private String keyword;
    private String songName;
    private String finished;
    private LocalDateTime createTime;
}
