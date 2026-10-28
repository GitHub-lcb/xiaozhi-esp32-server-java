package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "固件升级任务分页查询")
public class FirmwareReleasePageReq extends BasePageReq {

    @Schema(description = "任务名称，模糊匹配")
    private String releaseName;

    @Schema(description = "目标固件ID")
    private Integer firmwareId;

    @Schema(description = "任务状态(0待开始 1进行中 2已暂停 3已结束)")
    private String state;
}
