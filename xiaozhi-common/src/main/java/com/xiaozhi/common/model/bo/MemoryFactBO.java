package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 长期记忆事实：一句关于某位成员（或全家）的、值得跨会话记住的话。
 */
@Data
public class MemoryFactBO {

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_AUTO = "auto";
    public static final String STATE_ENABLED = "1";
    public static final String STATE_DISABLED = "0";

    private Long factId;
    private Integer userId;
    private String memberName;
    private String content;
    private String category;
    private String source;
    private String state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
