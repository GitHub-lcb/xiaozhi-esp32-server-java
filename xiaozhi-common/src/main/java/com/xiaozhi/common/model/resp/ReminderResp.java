package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "语音提醒")
public class ReminderResp {

    @Schema(description = "提醒ID")
    private Long reminderId;

    @Schema(description = "目标设备ID")
    private String deviceId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "播报内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下次播报时间")
    private LocalDateTime remindTime;

    @Schema(description = "重复方式")
    private String repeatType;

    @Schema(description = "状态(0待播报 1已播报 2已取消 3已错过)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后播报时间")
    private LocalDateTime lastFireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
