package com.xiaozhi.push.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.PushMessageBO;

public interface PushMessageService {

    PageResult<PushMessageBO> page(int pageNo, int pageSize, String title, String state);

    PushMessageBO getBO(Long pushId);

    PushMessageBO create(PushMessageBO bo);

    PushMessageBO update(PushMessageBO bo);

    /** 标记已发送，并记录下发/离线条数 */
    void markSent(Long pushId, int sentCount, int offlineCount, int totalCount);

    void delete(Long pushId);
}
