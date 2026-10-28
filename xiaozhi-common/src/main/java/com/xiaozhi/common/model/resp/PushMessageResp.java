package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "主动推送消息")
public class PushMessageResp {

    @Schema(description = "推送ID")
    private Long pushId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "播报内容")
    private String content;

    @Schema(description = "目标类型：device / user")
    private String targetType;

    @Schema(description = "目标值，逗号分隔")
    private String targetValue;

    @Schema(description = "状态(0待发送 1已发送 2已取消)")
    private String state;

    @Schema(description = "目标设备数")
    private Integer totalCount;

    @Schema(description = "已下发数")
    private Integer sentCount;

    @Schema(description = "离线跳过数")
    private Integer offlineCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发送时间")
    private LocalDateTime sentTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
