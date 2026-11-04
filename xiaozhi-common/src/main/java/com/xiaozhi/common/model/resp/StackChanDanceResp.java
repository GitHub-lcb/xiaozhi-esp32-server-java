package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 舞蹈响应，字段与 Go 版后端 /stackChan/dance 系列接口一致。
 * <p>
 * danceData 用 {@link JsonRawValue} 原样输出：库里存的就是 JSON 数组文本，
 * 不加这层的话会被 Jackson 转义成字符串，App/固件解析不到数组。
 */
@Data
@Schema(description = "StackChan 舞蹈")
public class StackChanDanceResp {

    @Schema(description = "舞蹈 ID")
    private Long id;

    @Schema(description = "所属设备 MAC（管理端使用；设备/App 接口也会返回）")
    private String mac;

    @Schema(description = "舞蹈名称")
    private String danceName;

    @Schema(description = "背景音乐地址")
    private String musicUrl;

    @JsonRawValue
    @Schema(description = "舞蹈动作数据，JSON 数组")
    private String danceData;
}
