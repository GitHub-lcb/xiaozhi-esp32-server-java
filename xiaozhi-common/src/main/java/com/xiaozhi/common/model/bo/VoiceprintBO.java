package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 声纹：一位家庭成员的说话人向量，用来在对话里区分"是谁在说话"。
 */
@Data
public class VoiceprintBO {

    public static final String STATE_ENABLED = "1";
    public static final String STATE_DISABLED = "0";

    private Long voiceprintId;
    private Integer userId;
    private String memberName;
    private List<Double> embedding;
    private BigDecimal sampleSeconds;
    private String state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
