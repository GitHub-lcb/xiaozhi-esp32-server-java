package com.xiaozhi.memorygraph.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_memory_relation")
public class MemoryRelationDO extends BaseDO {

    @TableId(value = "relationId", type = IdType.AUTO)
    private Long relationId;

    private Integer userId;
    private Long fromEntityId;
    private Long toEntityId;
    private String relation;
}
