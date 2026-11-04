package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan App Store 应用条目。
 */
@Data
public class StackChanAppBO {

    public static final Integer STATE_ON = 1;
    public static final Integer STATE_OFF = 0;

    public static final Integer NOT_DELETED = 0;
    public static final Integer DELETED = 1;

    private Long appId;

    private String appName;

    private String appIconUrl;

    private String description;

    private String firmwareUrl;

    /** 1 上架 / 0 下架 */
    private Integer state;

    /** 0 正常 / 1 已软删除 */
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
