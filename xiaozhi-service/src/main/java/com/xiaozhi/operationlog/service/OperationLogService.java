package com.xiaozhi.operationlog.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.OperationLogBO;

import java.time.LocalDateTime;

public interface OperationLogService {

    /**
     * 异步保存操作日志，不影响主流程性能。
     */
    void saveAsync(OperationLogBO log);

    /**
     * 分页查询操作日志，供管理端「操作日志」页面使用。
     *
     * @param module    模块，模糊匹配，可空
     * @param operation 操作描述，模糊匹配，可空
     * @param userId    操作人，可空
     * @param success   是否成功，可空
     * @param startTime 起始时间（含），可空
     * @param endTime   结束时间（含），可空
     */
    PageResult<OperationLogBO> page(int pageNo, int pageSize, String module, String operation,
                                    Integer userId, Boolean success,
                                    LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除超过保留期的操作日志，分批删除避免长事务锁表。
     *
     * @return 实际删除的行数
     */
    int deleteExpired(int retentionDays, int batchSize);
}
