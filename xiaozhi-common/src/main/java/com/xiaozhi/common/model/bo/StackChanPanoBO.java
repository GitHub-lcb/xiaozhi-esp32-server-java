package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 设备全景图。
 */
@Data
public class StackChanPanoBO {

    private Long id;

    /** 设备 MAC，统一小写冒号格式 */
    private String mac;

    /** 全景图地址 */
    private String panoUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
