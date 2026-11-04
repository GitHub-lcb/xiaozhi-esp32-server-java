package com.xiaozhi.stackchan.service;

import com.xiaozhi.common.model.bo.StackChanFriendBO;

import java.util.List;

/**
 * StackChan 设备好友关系读写。
 * <p>
 * Go 版后端的语义：加好友记录单向一条（macA 加 macB），但「已存在」要双向判断，
 * 任一方向已存在都按幂等成功处理，不重复插入。
 */
public interface StackChanFriendService {

    /**
     * 加好友：不能加自己；好友设备必须存在；双向已存在时幂等成功。
     * <p>
     * 两侧 MAC 都会归一化成小写冒号格式后再判断与落库。
     */
    void add(String mac, String friendMac);

    /** 判断两个设备之间是否已存在任一方向的好友关系 */
    boolean exists(String macA, String macB);

    /** 管理端列出全部好友关系 */
    List<StackChanFriendBO> listAll();

    /** 管理端删除好友关系，不存在时静默返回 */
    void delete(Long id);
}
