package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StackChan 社区帖子。
 * <p>
 * 帖子是公共时间线，按创建时间倒序分页返回；每条带自己的评论列表（正序）。
 * 设备名不在本 BO 里，由 server 侧的 Controller 经 DeviceService 补齐。
 */
@Data
public class StackChanPostBO {

    private Long id;

    /** 发帖设备 MAC，统一小写冒号格式 */
    private String mac;

    private String contentText;

    private String contentImage;

    private LocalDateTime createTime;

    /** 该帖子下的评论，按创建时间正序 */
    private List<StackChanPostCommentBO> postCommentList;
}
