package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "固件版本分页查询")
public class FirmwarePageReq extends BasePageReq {

    @Schema(description = "版本号，模糊匹配")
    private String version;

    @Schema(description = "芯片型号")
    private String chipModel;

    @Schema(description = "状态(1启用 0停用)")
    private String state;
}
