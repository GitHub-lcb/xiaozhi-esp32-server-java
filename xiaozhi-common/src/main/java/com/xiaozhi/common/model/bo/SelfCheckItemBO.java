package com.xiaozhi.common.model.bo;

import lombok.Data;

/**
 * 自检单项：一条检查的结论、依据和修复建议。
 */
@Data
public class SelfCheckItemBO {

    public static final String STATUS_PASS = "pass";
    public static final String STATUS_WARN = "warn";
    public static final String STATUS_FAIL = "fail";

    /** 稳定的检查项 key，前端按它做图标与文案映射 */
    private String key;
    private String name;
    private String status;
    /** 判定依据：实际取到的值，出问题时能直接看出是哪个地址/哪个字段不对 */
    private String detail;
    /** 失败或告警时给出的下一步动作 */
    private String suggestion;

    public static SelfCheckItemBO of(String key, String name, String status, String detail, String suggestion) {
        SelfCheckItemBO item = new SelfCheckItemBO();
        item.key = key;
        item.name = name;
        item.status = status;
        item.detail = detail;
        item.suggestion = suggestion;
        return item;
    }
}
