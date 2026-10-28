package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "语音提醒分页查询")
public class ReminderPageReq extends BasePageReq {

    @Schema(description = "设备ID，模糊匹配")
    private String deviceId;

    @Schema(description = "状态(0待播报 1已播报 2已取消 3已错过)")
    private String state;
}
