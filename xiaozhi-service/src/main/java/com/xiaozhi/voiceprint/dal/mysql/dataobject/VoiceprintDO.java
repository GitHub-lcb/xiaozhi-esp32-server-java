package com.xiaozhi.voiceprint.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_voiceprint")
public class VoiceprintDO extends BaseDO {

    @TableId(value = "voiceprintId", type = IdType.AUTO)
    private Long voiceprintId;

    private Integer userId;
    private String memberName;
    private String embedding;
    private BigDecimal sampleSeconds;
    private String state;
}
