package com.xiaozhi.operationlog.convert;

import com.xiaozhi.common.model.bo.OperationLogBO;
import com.xiaozhi.common.model.resp.OperationLogResp;
import com.xiaozhi.operationlog.dal.mysql.dataobject.OperationLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OperationLogConvert {

    OperationLogDO toDO(OperationLogBO bo);

    OperationLogBO toBO(OperationLogDO dataObject);

    /** 用户名不在审计表里，由 server 侧按 userId 回填。 */
    @Mapping(target = "username", ignore = true)
    OperationLogResp toResp(OperationLogBO bo);
}
