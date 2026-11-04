package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 好友关系，管理端展示用。
 */
@Data
public class StackChanFriendBO {

    private Long id;

    /** 发起方设备 MAC */
    private String macA;

    /** 好友设备 MAC */
    private String macB;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
