package com.xiaozhi.stats.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dialogue_metric")
public class DialogueMetricDO extends BaseDO {

    @TableId(value = "metricId", type = IdType.AUTO)
    private Long metricId;

    private String deviceId;
    private Integer roleId;
    private String sessionId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long latencyMs;
}
