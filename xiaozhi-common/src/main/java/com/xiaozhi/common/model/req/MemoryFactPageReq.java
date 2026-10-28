package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "长期记忆分页查询")
public class MemoryFactPageReq extends BasePageReq {

    @Schema(description = "内容，模糊匹配")
    private String content;

    @Schema(description = "成员称呼")
    private String memberName;
}
