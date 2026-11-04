package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 社区接口的「新建成功」响应：Go 版后端各 create 接口的 data 只有一个 id。
 */
@Data
@Schema(description = "StackChan 新建记录 ID")
public class StackChanIdResp {

    @Schema(description = "新建记录 ID")
    private Long id;
}
