package com.xiaozhi.music.service;

import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;

import java.util.List;

/**
 * 曲库：以文件系统为准，数据库只是元数据与统计。
 * <p>
 * 目录里多出来的文件（手工 scp 进去的）会在 {@link #listAndSync()} 时补登记；
 * 数据库里有但文件已删的，标记为下架而不是物理删除。
 */
public interface MusicLibraryService {

    /** 扫描音乐目录，把新文件补登记入库，返回全部可用歌曲 */
    List<MusicBO> listAndSync();

    /** 按用户说法匹配歌曲：别名 → 歌名 → 文件名包含；找不到返回 null */
    MusicBO match(String keyword);

    /** 上传后登记（解析歌名/歌手/时长） */
    MusicBO register(String fileName);

    /** 下架（不删文件） */
    void remove(String fileName);

    /** 记一次点播 */
    void logPlay(String deviceId, MusicBO music, String keyword);

    /** 最近点播记录 */
    List<MusicPlayLogBO> recentPlays(int limit);
}
