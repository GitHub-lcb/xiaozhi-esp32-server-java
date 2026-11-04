package com.xiaozhi.stackchan.service;

import com.xiaozhi.common.model.bo.StackChanDanceBO;

import java.util.List;

/**
 * StackChan 设备舞蹈的读写：设备侧接口按当前设备 MAC 操作，App 侧接口按传入 MAC 操作。
 * <p>
 * 两侧共用同一份数据，写入口径一致——mac 统一归一化成小写冒号格式后再落库。
 */
public interface StackChanDanceService {

    /** 按设备 MAC 列出舞蹈，按创建顺序（danceId 升序）返回 */
    List<StackChanDanceBO> listByMac(String mac);

    /** 管理端列出全部舞蹈；mac 为空表示不过滤设备 */
    List<StackChanDanceBO> listAll(String mac);

    /** 按舞蹈 ID 查询，不存在返回 null */
    StackChanDanceBO get(Long danceId);

    /**
     * 新增舞蹈：mac、danceName、danceData 必填；同一设备下同名舞蹈视为重复。
     */
    StackChanDanceBO create(StackChanDanceBO bo);

    /**
     * 局部更新：BO 里为 null 的字段保持不变（与 Go 版 PUT 语义一致，空串同样按未提供处理）。
     */
    StackChanDanceBO update(Long danceId, StackChanDanceBO bo);

    /** 删除舞蹈；ID 不存在时静默返回，便于客户端重复删除 */
    void delete(Long danceId);
}
