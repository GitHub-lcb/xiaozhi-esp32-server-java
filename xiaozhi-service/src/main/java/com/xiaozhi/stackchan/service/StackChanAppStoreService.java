package com.xiaozhi.stackchan.service;

import com.xiaozhi.common.model.bo.StackChanAppBO;

import java.util.List;

/**
 * StackChan App Store 应用条目的读写。
 * <p>
 * 公开列表只给固件/App 看已上架且未删除的条目；管理端列表看未删除的全部条目（含下架），
 * 删除一律软删除，避免已经装到设备上的应用记录被物理删掉。
 */
public interface StackChanAppStoreService {

    /** 公开列表：state=1 且未删除，按 appId 升序 */
    List<StackChanAppBO> listPublicApps();

    /** 管理端列表：全部未删除条目，按 appId 倒序 */
    List<StackChanAppBO> listAdminApps();

    /** 按应用 ID 查询未删除条目，不存在返回 null */
    StackChanAppBO get(Long appId);

    /** 新增应用：appName 必填，state 缺省为 1（上架） */
    StackChanAppBO create(StackChanAppBO bo);

    /** 局部更新：BO 里为 null 的字段保持不变 */
    StackChanAppBO update(Long appId, StackChanAppBO bo);

    /** 软删除；已删除或不存在时静默返回 */
    void delete(Long appId);
}
