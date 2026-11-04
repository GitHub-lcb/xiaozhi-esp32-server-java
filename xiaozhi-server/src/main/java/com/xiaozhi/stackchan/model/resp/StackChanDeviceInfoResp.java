package com.xiaozhi.stackchan.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * StackChan 设备信息（对应 Go 版 {@code model.DeviceInfo}）。
 * <p>
 * 字段顺序与 Go 一致：mac / name / uid / bind_time。固件的 Setup 页只读 {@code name}，
 * 其余字段是给 App 与调试用的；老固件解析时不认识多出来的字段会自动忽略，向后兼容。
 */
@Data
@Schema(description = "StackChan 设备信息")
public class StackChanDeviceInfoResp {

    @Schema(description = "设备 MAC（小写冒号格式）", example = "aa:bb:cc:dd:ee:ff")
    private String mac;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "绑定用户 ID", example = "1")
    private Integer uid;

    @JsonProperty("bind_time")
    @Schema(description = "绑定时间，格式 yyyy-MM-dd HH:mm:ss")
    private String bindTime;
}
