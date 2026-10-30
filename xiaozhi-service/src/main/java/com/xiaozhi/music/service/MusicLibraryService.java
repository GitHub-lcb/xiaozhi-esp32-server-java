package com.xiaozhi.music.service;

import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;
import com.xiaozhi.common.model.bo.MusicStatsBO;
import com.xiaozhi.common.model.bo.MusicSyncBO;

import java.util.List;

/**
 * 曲库：以文件系统为准，数据库只是元数据与统计。
 * <p>
 * 目录里多出来的文件（手工 scp 进去的）会在 {@link #listAndSync()} / {@link #page} 时补登记；
 * 数据库里有但文件已删的，标记为下架而不是物理删除，点播历史因此不会丢。
 * <p>
 * 前半部分是对话链路（dialogue 的 LocalMusicPlayer）在用的方法，签名不要动；
 * 后半部分是后台曲库管理页 v2 的读写接口。
 */
public interface MusicLibraryService {

    // ===================== 对话链路：LocalMusicPlayer 依赖，签名保持稳定 =====================

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

    // ===================== 后台曲库页 =====================

    /** 分页查询；查询前先做轻量同步，把目录里新增的 mp3 补登记。pageNo/pageSize 为空时取默认值 */
    PageResult<MusicBO> page(Integer pageNo, Integer pageSize, String keyword, String state, String sortBy, String sortOrder);

    /** 曲目总数/上下架数/点播与占用统计 */
    MusicStatsBO stats();

    /** 点播记录分页，设备名与歌名在返回前补齐。pageNo/pageSize 为空时取默认值 */
    PageResult<MusicPlayLogBO> pagePlays(Integer pageNo, Integer pageSize, String keyword, String deviceId);

    /** 编辑歌名/歌手/别名/上下架；patch 里的 null 字段表示保持原值 */
    MusicBO update(Long musicId, MusicBO patch);

    /** 删除磁盘文件并把记录下架，保留点播历史 */
    void delete(Long musicId);

    /** 扫描目录：新文件登记、文件消失的记录下架 */
    MusicSyncBO sync();

    /** 试听地址：返回相对 dataDir、带时效签名的存储键 */
    String audioUrl(Long musicId);
}
