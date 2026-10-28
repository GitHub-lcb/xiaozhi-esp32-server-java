package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "音色克隆记录")
public class VoiceCloneResp {

    @Schema(description = "克隆ID")
    private Long cloneId;

    @Schema(description = "音色名称")
    private String voiceName;

    @Schema(description = "服务商")
    private String provider;

    @Schema(description = "服务商音色ID")
    private String voiceId;

    @Schema(description = "状态(0处理中 1就绪 2失败)")
    private String state;

    @Schema(description = "失败原因")
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
