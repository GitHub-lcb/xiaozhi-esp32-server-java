package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主动推送分页查询")
public class PushMessagePageReq extends BasePageReq {

    @Schema(description = "标题，模糊匹配")
    private String title;

    @Schema(description = "状态(0待发送 1已发送 2已取消)")
    private String state;
}
