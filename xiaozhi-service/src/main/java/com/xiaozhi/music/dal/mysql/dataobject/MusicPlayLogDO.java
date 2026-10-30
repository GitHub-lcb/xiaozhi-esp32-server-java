package com.xiaozhi.music.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_music_play_log")
public class MusicPlayLogDO extends BaseDO {

    @TableId(value = "logId", type = IdType.AUTO)
    private Long logId;

    private String deviceId;
    private Long musicId;
    private String keyword;
    private String songName;
    private String finished;
}
