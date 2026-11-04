package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 好友关系（管理端展示）。
 */
@Data
@Schema(description = "StackChan 好友关系")
public class StackChanFriendResp {

    @Schema(description = "关系 ID")
    private Long id;

    @Schema(description = "发起方设备 MAC")
    private String macA;

    @Schema(description = "好友设备 MAC")
    private String macB;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
