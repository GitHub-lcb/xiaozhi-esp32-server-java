package com.xiaozhi.memoryfact.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MemoryFactBO;

import java.util.List;

public interface MemoryFactService {

    PageResult<MemoryFactBO> page(int pageNo, int pageSize, String content, String memberName, Integer userId);

    MemoryFactBO create(MemoryFactBO bo);

    void delete(Long factId, Integer userId);

    /** 某位说话人（或全家）当前生效的记忆，供对话注入 */
    List<MemoryFactBO> listForPrompt(Integer userId, String memberName, int limit);
}
