package com.xiaozhi.memoryfact.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_memory_fact")
public class MemoryFactDO extends BaseDO {

    @TableId(value = "factId", type = IdType.AUTO)
    private Long factId;

    private Integer userId;
    private String memberName;
    private String content;
    private String category;
    private String source;
    private String state;
}
