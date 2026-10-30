package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "曲库点播记录")
public class MusicPlayLogResp {

    @Schema(description = "记录ID")
    private Long logId;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备名，设备已解绑时为 null")
    private String deviceName;

    @Schema(description = "曲目ID，未命中曲库时为 null")
    private Long musicId;

    @Schema(description = "实际播放的文件名")
    private String songName;

    @Schema(description = "命中曲目的歌名，曲目记录不存在时为 null")
    private String musicTitle;

    @Schema(description = "用户的点播说法")
    private String keyword;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "点播时间")
    private LocalDateTime createTime;
}
