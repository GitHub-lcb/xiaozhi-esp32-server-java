package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "声纹")
public class VoiceprintResp {

    @Schema(description = "声纹ID")
    private Long voiceprintId;

    @Schema(description = "成员称呼")
    private String memberName;

    @Schema(description = "样本时长(秒)")
    private BigDecimal sampleSeconds;

    @Schema(description = "状态(1启用 0停用)")
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
