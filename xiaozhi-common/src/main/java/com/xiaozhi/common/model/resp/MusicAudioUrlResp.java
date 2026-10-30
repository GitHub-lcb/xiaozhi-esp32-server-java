package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 试听地址只下发签名 URL，音频流仍由静态资源处理器直出并支持 Range。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "曲目试听地址")
public class MusicAudioUrlResp {

    @Schema(description = "带时效签名的音频地址，前端交给 audio 标签即可")
    private String url;
}
