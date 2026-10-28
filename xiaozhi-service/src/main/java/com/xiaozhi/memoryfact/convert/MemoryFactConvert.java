package com.xiaozhi.memoryfact.convert;

import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.common.model.resp.MemoryFactResp;
import com.xiaozhi.memoryfact.dal.mysql.dataobject.MemoryFactDO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemoryFactConvert {

    MemoryFactBO toBO(MemoryFactDO dataObject);

    MemoryFactResp toResp(MemoryFactBO bo);
}
