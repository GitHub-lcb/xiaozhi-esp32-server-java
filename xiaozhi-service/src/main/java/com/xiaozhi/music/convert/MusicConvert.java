package com.xiaozhi.music.convert;

import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;
import com.xiaozhi.common.model.bo.MusicStatsBO;
import com.xiaozhi.common.model.bo.MusicSyncBO;
import com.xiaozhi.common.model.req.MusicUpdateReq;
import com.xiaozhi.common.model.resp.MusicPlayLogResp;
import com.xiaozhi.common.model.resp.MusicResp;
import com.xiaozhi.common.model.resp.MusicStatsResp;
import com.xiaozhi.common.model.resp.MusicSyncResp;
import com.xiaozhi.music.dal.mysql.dataobject.MusicDO;
import com.xiaozhi.music.dal.mysql.dataobject.MusicPlayLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 曲库 DO/BO/Resp 的字段搬运。
 * <p>
 * 点播记录里的设备名与歌名是查询时现补的展示字段，库里没有对应列，转换时不能去猜。
 */
@Mapper(componentModel = "spring")
public interface MusicConvert {

    MusicBO toBO(MusicDO dataObject);

    MusicResp toResp(MusicBO bo);

    MusicStatsResp toResp(MusicStatsBO bo);

    MusicSyncResp toResp(MusicSyncBO bo);

    @Mapping(target = "deviceName", ignore = true)
    @Mapping(target = "musicTitle", ignore = true)
    MusicPlayLogBO toBO(MusicPlayLogDO dataObject);

    MusicPlayLogResp toResp(MusicPlayLogBO bo);

    /**
     * 编辑弹窗只提交歌名/歌手/别名/状态四个字段，其余列保持原值：
     * 未提交的字段映射成 null，Service 侧据此判断「不修改」，不能把文件名、时长这些列清空。
     */
    @Mapping(target = "musicId", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "durationSec", ignore = true)
    @Mapping(target = "sizeBytes", ignore = true)
    @Mapping(target = "playCount", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    MusicBO toBO(MusicUpdateReq req);
}
