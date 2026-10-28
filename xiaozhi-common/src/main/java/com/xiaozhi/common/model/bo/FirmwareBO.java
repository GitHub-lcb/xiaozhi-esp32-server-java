package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件版本。
 */
@Data
public class FirmwareBO {

    public static final String STATE_ENABLED = "1";
    public static final String STATE_DISABLED = "0";

    private Integer firmwareId;
    private String version;
    /** 适用芯片型号，空表示不限 */
    private String chipModel;
    private String fileUrl;
    private Long fileSize;
    private String md5;
    private String releaseNotes;
    private String state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isEnabled() {
        return STATE_ENABLED.equals(state);
    }
}
