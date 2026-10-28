package com.xiaozhi.memorygraph.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_memory_entity")
public class MemoryEntityDO extends BaseDO {

    @TableId(value = "entityId", type = IdType.AUTO)
    private Long entityId;

    private Integer userId;
    private String name;
    private String type;
}
