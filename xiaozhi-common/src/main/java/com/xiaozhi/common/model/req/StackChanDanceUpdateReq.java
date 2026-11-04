package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "StackChan 舞蹈更新请求")
public class StackChanDanceUpdateReq {

    @Schema(description = "舞蹈 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "舞蹈ID不能为空")
    private Long id;

    @Schema(description = "舞蹈名称，不传表示不修改")
    private String danceName;

    @Schema(description = "舞蹈动作数据，JSON 数组原样存取，不传表示不修改")
    private JsonNode danceData;

    @Schema(description = "背景音乐地址，不传表示不修改")
    private String musicUrl;
}
