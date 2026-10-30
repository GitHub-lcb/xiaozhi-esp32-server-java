package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "曲目编辑")
public class MusicUpdateReq {

    @NotBlank(message = "歌名不能为空")
    @Size(max = 100, message = "歌名最长 100 个字符")
    @Schema(description = "歌名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 100, message = "歌手最长 100 个字符")
    @Schema(description = "歌手")
    private String artist;

    @Size(max = 300, message = "别名最长 300 个字符")
    @Schema(description = "别名，英文逗号分隔")
    private String aliases;

    /** 只收 "1"/"0"，避免把 state 写成别的值后列表按状态筛不出来 */
    @Pattern(regexp = "0|1", message = "状态只能是 1（上架）或 0（下架）")
    @Schema(description = "状态：1 已上架，0 已下架")
    private String state;
}
