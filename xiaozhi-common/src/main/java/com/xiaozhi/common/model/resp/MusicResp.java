package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "曲目")
public class MusicResp {

    @Schema(description = "曲目ID")
    private Long musicId;

    @Schema(description = "磁盘文件名")
    private String fileName;

    @Schema(description = "歌名")
    private String title;

    @Schema(description = "歌手")
    private String artist;

    @Schema(description = "别名，英文逗号分隔")
    private String aliases;

    @Schema(description = "时长(秒)")
    private Integer durationSec;

    @Schema(description = "文件大小(字节)")
    private Long sizeBytes;

    @Schema(description = "点播次数")
    private Integer playCount;

    @Schema(description = "状态(1已上架 0已下架)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
