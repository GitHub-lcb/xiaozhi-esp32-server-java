package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "StackChan 舞蹈创建请求")
public class StackChanDanceCreateReq {

    @Schema(description = "设备 MAC；设备侧接口以 Device-Id 为准，App 侧必传（支持 12 位纯十六进制或带分隔符）")
    private String mac;

    @Schema(description = "舞蹈名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "舞蹈名称不能为空")
    private String danceName;

    @Schema(description = "舞蹈动作数据，JSON 数组原样存取", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "舞蹈动作数据不能为空")
    private JsonNode danceData;

    @Schema(description = "背景音乐地址")
    private String musicUrl;
}
