package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "自检单项结果")
public class SelfCheckItemResp {

    @Schema(description = "检查项标识")
    private String key;

    @Schema(description = "检查项名称")
    private String name;

    @Schema(description = "结论：pass / warn / fail")
    private String status;

    @Schema(description = "判定依据")
    private String detail;

    @Schema(description = "修复建议")
    private String suggestion;
}
