package com.xiaozhi.stackchan.service;

import com.xiaozhi.common.model.bo.StackChanPanoBO;

import java.util.List;

/**
 * StackChan 设备全景图读写：设备侧只操作自己 MAC 名下的全景图。
 * <p>
 * 与 Go 版后端一致，不做更新与删除，只追加和按设备列举。
 */
public interface StackChanPanoService {

    /** 按设备 MAC 列出全景图，按创建顺序（id 升序）返回 */
    List<StackChanPanoBO> listByMac(String mac);

    /** 管理端列出全部全景图 */
    List<StackChanPanoBO> listAll();

    /** 管理端删除全景图，不存在时静默返回 */
    void delete(Long panoId);

    /** 为设备追加一张全景图：mac、panoUrl 必填 */
    StackChanPanoBO create(String mac, String panoUrl);
}
