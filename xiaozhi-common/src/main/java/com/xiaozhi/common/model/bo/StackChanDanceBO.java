package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * StackChan 设备舞蹈。
 * <p>
 * danceData 是动作 JSON 数组原文（固件/App 直接解析），读写都按字符串原样搬运，
 * 避免序列化层把数组改写成别的结构。
 */
@Data
public class StackChanDanceBO {

    private Long danceId;

    /** 设备 MAC，统一小写冒号格式 */
    private String mac;

    private String danceName;

    /** 动作数据 JSON 数组原文 */
    private String danceData;

    private String musicUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
