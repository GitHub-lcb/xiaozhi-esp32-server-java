package com.xiaozhi.stackchan.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StackChan App Store 应用条目，由管理端维护、固件公开拉取。
 * <p>
 * 删除走软删除（isDeleted=1），保证历史固件里引用的条目不会突然变成脏数据；
 * state 控制是否对固件公开（1 上架 / 0 下架）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stackchan_app")
public class StackChanAppDO extends BaseDO {

    @TableId(value = "appId", type = IdType.AUTO)
    private Long appId;

    private String appName;

    private String appIconUrl;

    private String description;

    private String firmwareUrl;

    /** 1 上架 / 0 下架 */
    private Integer state;

    /** 0 正常 / 1 已软删除 */
    private Integer isDeleted;
}
