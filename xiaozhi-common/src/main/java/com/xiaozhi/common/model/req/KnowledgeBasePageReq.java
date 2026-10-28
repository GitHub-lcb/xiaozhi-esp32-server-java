package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库分页查询")
public class KnowledgeBasePageReq extends BasePageReq {

    @Schema(description = "名称，模糊匹配")
    private String name;
}
