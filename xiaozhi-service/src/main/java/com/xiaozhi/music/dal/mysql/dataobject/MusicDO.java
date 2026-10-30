package com.xiaozhi.music.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_music")
public class MusicDO extends BaseDO {

    @TableId(value = "musicId", type = IdType.AUTO)
    private Long musicId;

    private String fileName;
    private String title;
    private String artist;
    private String aliases;
    private Integer durationSec;
    private Long sizeBytes;
    private Integer playCount;
    private String state;
}
