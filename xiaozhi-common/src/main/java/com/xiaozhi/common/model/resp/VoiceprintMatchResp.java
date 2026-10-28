package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "声纹识别结果")
public class VoiceprintMatchResp {

    @Schema(description = "命中的声纹ID，null 表示没认出来")
    private Long voiceprintId;

    @Schema(description = "命中的成员称呼")
    private String memberName;

    @Schema(description = "相似度 0-1")
    private Double score;

    @Schema(description = "是否达到阈值")
    private Boolean matched;
}
