package com.xiaozhi.music;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;
import com.xiaozhi.common.model.bo.MusicStatsBO;
import com.xiaozhi.common.model.bo.MusicSyncBO;
import com.xiaozhi.common.web.LocalFileUrlPolicy;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.music.convert.MusicConvert;
import com.xiaozhi.music.dal.mysql.dataobject.MusicDO;
import com.xiaozhi.music.dal.mysql.dataobject.MusicPlayLogDO;
import com.xiaozhi.music.dal.mysql.mapper.MusicMapper;
import com.xiaozhi.music.dal.mysql.mapper.MusicPlayLogMapper;
import com.xiaozhi.music.service.impl.MusicLibraryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 曲库服务：文件系统与数据库两条真相源的交界处都收在这里。
 * <p>
 * mapper 用 Mockito 顶掉，音乐目录用 @TempDir 真空目录，这样既能断言"文件真的被删/被登记"，
 * 又不用连 MySQL；切分页/排序的白名单由 Controller 用例与契约测试保障，这里只钉业务语义。
 */
class MusicLibraryServiceImplTest {

    @TempDir
    Path tempDir;

    private Path musicDir;

    private MusicMapper musicMapper;
    private MusicPlayLogMapper musicPlayLogMapper;
    private DeviceService deviceService;
    private LocalFileUrlPolicy localFileUrlPolicy;
    private RuntimePathConfig runtimePathConfig;
    private MusicLibraryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        musicDir = Files.createDirectories(tempDir.resolve("uploads").resolve("music"));
        musicMapper = mock(MusicMapper.class);
        musicPlayLogMapper = mock(MusicPlayLogMapper.class);
        deviceService = mock(DeviceService.class);
        localFileUrlPolicy = mock(LocalFileUrlPolicy.class);

        runtimePathConfig = new RuntimePathConfig();
        runtimePathConfig.setMusicDir(musicDir.toString());
        runtimePathConfig.setDataDir(tempDir.toString());

        service = new MusicLibraryServiceImpl();
        ReflectionTestUtils.setField(service, "musicMapper", musicMapper);
        ReflectionTestUtils.setField(service, "musicPlayLogMapper", musicPlayLogMapper);
        ReflectionTestUtils.setField(service, "runtimePathConfig", runtimePathConfig);
        ReflectionTestUtils.setField(service, "deviceService", deviceService);
        ReflectionTestUtils.setField(service, "localFileUrlPolicy", localFileUrlPolicy);
        ReflectionTestUtils.setField(service, "musicConvert", Mappers.getMapper(MusicConvert.class));
    }

    // ===================== 登记 =====================

    @Test
    void registerCreatesRowWithTitleArtistAndFileMetadata() throws Exception {
        Path file = musicDir.resolve("周杰伦 - 稻香.mp3");
        Files.write(file, "not-a-real-mp3".getBytes());
        when(musicMapper.selectOne(any())).thenReturn(null);

        MusicBO result = service.register("周杰伦 - 稻香.mp3");

        assertThat(result.getTitle()).isEqualTo("稻香");
        assertThat(result.getArtist()).isEqualTo("周杰伦");
        assertThat(result.getAliases()).isEqualTo("稻香");
        assertThat(result.getState()).isEqualTo("1");
        assertThat(result.getSizeBytes()).isEqualTo(Files.size(file));

        ArgumentCaptor<MusicDO> captor = ArgumentCaptor.forClass(MusicDO.class);
        verify(musicMapper).insert(captor.capture());
        assertThat(captor.getValue().getFileName()).isEqualTo("周杰伦 - 稻香.mp3");
        assertThat(captor.getValue().getPlayCount()).isZero();
    }

    @Test
    void registerKeepsManuallyEditedTitleAndReEnablesExistingRow() throws Exception {
        Path file = musicDir.resolve("周杰伦 - 稻香.mp3");
        Files.write(file, "abc".getBytes());
        MusicDO existing = row(5L, "周杰伦 - 稻香.mp3", "人工改过的歌名", "0");
        existing.setArtist("人工歌手");
        existing.setAliases("人工别名");
        when(musicMapper.selectOne(any())).thenReturn(existing);

        MusicBO result = service.register("周杰伦 - 稻香.mp3");

        assertThat(result.getTitle()).isEqualTo("人工改过的歌名");
        assertThat(result.getArtist()).isEqualTo("人工歌手");
        assertThat(result.getAliases()).isEqualTo("人工别名");
        // 文件回到目录里说明这首歌重新可播，登记时把下架状态改回上架
        assertThat(result.getState()).isEqualTo("1");
        verify(musicMapper).updateById(existing);
        verify(musicMapper, never()).insert(any(MusicDO.class));
    }

    // ===================== 匹配 =====================

    @Test
    void matchPrefersExactHitThenShortestContains() {
        MusicDO live = row(1L, "稻香 - 现场版.mp3", "稻香现场版", "1");
        MusicDO exact = row(2L, "周杰伦 - 稻香.mp3", "稻香", "1");
        exact.setAliases("稻香,周杰伦");
        when(musicMapper.selectList(any())).thenReturn(List.of(live, exact));

        assertThat(service.match("稻香").getMusicId()).isEqualTo(2L);
        // 没有精确命中时走包含匹配，取标题最短的，避免点"稻"放成"稻香现场版"
        assertThat(service.match("稻").getMusicId()).isEqualTo(2L);
        assertThat(service.match("周杰伦").getMusicId()).isEqualTo(2L);
    }

    @Test
    void matchReturnsNullOnBlankQueryOrUnknownSong() {
        MusicDO song = row(1L, "周杰伦 - 稻香.mp3", "稻香", "1");
        when(musicMapper.selectList(any())).thenReturn(List.of(song));

        assertThat(service.match(" ")).isNull();
        assertThat(service.match("完全不存在的歌")).isNull();
    }

    // ===================== 分页 / 统计 =====================

    @Test
    void pageRegistersNewDirectoryFilesAndClampsPageSize() throws Exception {
        Files.write(musicDir.resolve("新歌.mp3"), "abc".getBytes());
        MusicDO existing = row(1L, "旧歌.mp3", "旧歌", "1");
        when(musicMapper.selectList(any())).thenReturn(List.of(existing));
        when(musicMapper.selectOne(any())).thenReturn(null);
        when(musicMapper.<IPage<MusicDO>>selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<MusicDO> page = invocation.getArgument(0);
            page.setRecords(List.of(existing));
            page.setTotal(1L);
            return page;
        });

        PageResult<MusicBO> result = service.page(1, 500, "旧", "1", "playCount", "desc");

        // 契约规定单页上限 100，传入 500 要被收窄
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).singleElement().extracting(MusicBO::getTitle).isEqualTo("旧歌");
        // 列表前的轻量同步把目录里新增的 mp3 补登记了
        verify(musicMapper).insert(any(MusicDO.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<MusicDO>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(musicMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void pageDefaultsPageSizeAndPageNoWhenOutOfRange() {
        when(musicMapper.selectList(any())).thenReturn(List.of());
        when(musicMapper.<IPage<MusicDO>>selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<MusicDO> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0L);
            return page;
        });

        PageResult<MusicBO> zero = service.page(0, 0, null, null, null, null);
        assertThat(zero.getPageNo()).isEqualTo(1);
        assertThat(zero.getPageSize()).isEqualTo(10);

        // 空串查询参数在控制器侧会绑定成 null，Service 必须自己兜底而不是拆箱 NPE
        PageResult<MusicBO> nulls = service.page(null, null, null, null, null, null);
        assertThat(nulls.getPageNo()).isEqualTo(1);
        assertThat(nulls.getPageSize()).isEqualTo(10);
    }

    @Test
    void statsAggregatesTotalsAndTodayPlays() {
        MusicDO enabled = row(1L, "a.mp3", "A", "1");
        enabled.setPlayCount(2);
        enabled.setDurationSec(100);
        enabled.setSizeBytes(1000L);
        MusicDO disabled = row(2L, "b.mp3", "B", "0");
        disabled.setPlayCount(3);
        disabled.setDurationSec(200);
        disabled.setSizeBytes(2000L);
        MusicDO empty = row(3L, "c.mp3", "C", "1");
        empty.setPlayCount(null);
        empty.setDurationSec(null);
        empty.setSizeBytes(null);
        when(musicMapper.selectList(any())).thenReturn(List.of(enabled, disabled, empty));
        when(musicPlayLogMapper.selectCount(any())).thenReturn(5L);

        MusicStatsBO stats = service.stats();

        assertThat(stats.getTotal()).isEqualTo(3L);
        assertThat(stats.getEnabled()).isEqualTo(2L);
        assertThat(stats.getDisabled()).isEqualTo(1L);
        assertThat(stats.getTotalPlayCount()).isEqualTo(5L);
        assertThat(stats.getTotalDurationSec()).isEqualTo(300L);
        assertThat(stats.getTotalSizeBytes()).isEqualTo(3000L);
        assertThat(stats.getTodayPlayCount()).isEqualTo(5L);
    }

    // ===================== 点播记录 =====================

    @Test
    void pagePlaysFillsDeviceNameAndMusicTitle() {
        MusicPlayLogDO log = playLog(100L, "aa:bb:cc", 3L);
        when(musicPlayLogMapper.<IPage<MusicPlayLogDO>>selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<MusicPlayLogDO> page = invocation.getArgument(0);
            page.setRecords(List.of(log));
            page.setTotal(1L);
            return page;
        });
        DeviceBO device = new DeviceBO();
        device.setDeviceId("aa:bb:cc");
        device.setDeviceName("客厅音箱");
        when(deviceService.listByDeviceIds(anyList())).thenReturn(List.of(device));
        when(musicMapper.selectList(any())).thenReturn(List.of(row(3L, "稻香.mp3", "稻香", "1")));

        PageResult<MusicPlayLogBO> result = service.pagePlays(1, 10, "稻香", null);

        assertThat(result.getList()).singleElement().satisfies(item -> {
            assertThat(item.getDeviceName()).isEqualTo("客厅音箱");
            assertThat(item.getMusicTitle()).isEqualTo("稻香");
            assertThat(item.getSongName()).isEqualTo("稻香.mp3");
            assertThat(item.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 10, 30, 12, 0, 0));
        });
    }

    @Test
    void pagePlaysLeavesNamesNullWhenDeviceAndMusicAreGone() {
        when(musicPlayLogMapper.<IPage<MusicPlayLogDO>>selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<MusicPlayLogDO> page = invocation.getArgument(0);
            page.setRecords(List.of(playLog(101L, "已解绑设备", 404L)));
            page.setTotal(1L);
            return page;
        });
        when(deviceService.listByDeviceIds(anyList())).thenReturn(List.of());
        when(musicMapper.selectList(any())).thenReturn(List.of());

        MusicPlayLogBO item = service.pagePlays(1, 10, null, "已解绑设备").getList().get(0);

        assertThat(item.getDeviceName()).isNull();
        assertThat(item.getMusicTitle()).isNull();
    }

    // ===================== 编辑 / 删除 =====================

    @Test
    void updateOnlyOverwritesProvidedFields() {
        MusicDO existing = row(3L, "周杰伦 - 稻香.mp3", "旧歌名", "1");
        existing.setArtist("周杰伦");
        existing.setAliases("旧别名");
        when(musicMapper.selectById(3L)).thenReturn(existing);

        MusicBO patch = new MusicBO();
        patch.setTitle("新歌名");
        patch.setState("0");
        MusicBO result = service.update(3L, patch);

        assertThat(result.getTitle()).isEqualTo("新歌名");
        assertThat(result.getState()).isEqualTo("0");
        // 请求里没给的歌手/别名不能被清空
        assertThat(result.getArtist()).isEqualTo("周杰伦");
        assertThat(result.getAliases()).isEqualTo("旧别名");
        assertThat(result.getUpdateTime()).isNotNull();
        verify(musicMapper).updateById(existing);
    }

    @Test
    void updateMissingMusicThrowsNotFound() {
        when(musicMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, new MusicBO()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("曲目不存在");
    }

    @Test
    void deleteRemovesDiskFileAndUnlistsRowToKeepHistory() throws Exception {
        Path file = musicDir.resolve("稻香.mp3");
        Files.write(file, "abc".getBytes());
        when(musicMapper.selectById(3L)).thenReturn(row(3L, "稻香.mp3", "稻香", "1"));

        service.delete(3L);

        assertThat(Files.exists(file)).isFalse();
        ArgumentCaptor<MusicDO> captor = ArgumentCaptor.forClass(MusicDO.class);
        verify(musicMapper).updateById(captor.capture());
        assertThat(captor.getValue().getMusicId()).isEqualTo(3L);
        assertThat(captor.getValue().getState()).isEqualTo("0");
        // 只下架不物理删行，否则点播历史会变成查不到歌名的孤儿记录
        verify(musicMapper).selectById(3L);
        verifyNoMoreInteractions(musicMapper);
    }

    @Test
    void deleteKeepsGoingWhenFileAlreadyGone() {
        when(musicMapper.selectById(3L)).thenReturn(row(3L, "早就没了.mp3", "早就没了", "1"));

        service.delete(3L);

        ArgumentCaptor<MusicDO> captor = ArgumentCaptor.forClass(MusicDO.class);
        verify(musicMapper).updateById(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo("0");
    }

    @Test
    void deleteMissingMusicThrowsNotFound() {
        when(musicMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("曲目不存在");
    }

    // ===================== 同步 =====================

    @Test
    void syncRegistersAddedAndUnlistsRemoved() throws Exception {
        Files.write(musicDir.resolve("在场.mp3"), "abc".getBytes());
        Files.write(musicDir.resolve("新增.mp3"), "abc".getBytes());
        MusicDO present = row(1L, "在场.mp3", "在场", "1");
        MusicDO missing = row(2L, "已删.mp3", "已删", "1");
        MusicDO alreadyOff = row(3L, "早已下架.mp3", "早已下架", "0");
        when(musicMapper.selectList(any())).thenReturn(List.of(present, missing, alreadyOff));
        when(musicMapper.selectOne(any())).thenReturn(null);
        when(musicMapper.selectCount(any())).thenReturn(10L);

        MusicSyncBO result = service.sync();

        assertThat(result.getAdded()).isEqualTo(1);
        // 本来就下架的记录不再重复计入 removed
        assertThat(result.getRemoved()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(10L);
        verify(musicMapper).insert(any(MusicDO.class));
        ArgumentCaptor<MusicDO> captor = ArgumentCaptor.forClass(MusicDO.class);
        verify(musicMapper).updateById(captor.capture());
        assertThat(captor.getValue().getMusicId()).isEqualTo(2L);
        assertThat(captor.getValue().getState()).isEqualTo("0");
    }

    @Test
    void syncSkipsEverythingWhenMusicDirMissing() {
        runtimePathConfig.setMusicDir(tempDir.resolve("不存在的目录").toString());
        when(musicMapper.selectList(any())).thenReturn(List.of(row(1L, "a.mp3", "A", "1")));
        when(musicMapper.selectCount(any())).thenReturn(3L);

        MusicSyncBO result = service.sync();

        // 目录不可读时全库下架会把一次配置错误放大成数据事故，这里必须什么都不做
        assertThat(result.getAdded()).isZero();
        assertThat(result.getRemoved()).isZero();
        assertThat(result.getTotal()).isEqualTo(3L);
        verify(musicMapper, never()).updateById(any(MusicDO.class));
    }

    // ===================== 试听地址 =====================

    @Test
    void audioUrlSignsPathRelativeToDataDir() throws Exception {
        Files.write(musicDir.resolve("稻香.mp3"), "abc".getBytes());
        when(musicMapper.selectById(3L)).thenReturn(row(3L, "稻香.mp3", "稻香", "1"));
        when(localFileUrlPolicy.sign("uploads/music/稻香.mp3"))
            .thenReturn("uploads/music/稻香.mp3?exp=1&sig=abc");

        assertThat(service.audioUrl(3L)).isEqualTo("uploads/music/稻香.mp3?exp=1&sig=abc");
        verify(localFileUrlPolicy).sign("uploads/music/稻香.mp3");
    }

    @Test
    void audioUrlRejectsMusicDirOutsideDataDir() throws Exception {
        Files.write(musicDir.resolve("稻香.mp3"), "abc".getBytes());
        runtimePathConfig.setDataDir(tempDir.resolve("data").toString());
        when(musicMapper.selectById(3L)).thenReturn(row(3L, "稻香.mp3", "稻香", "1"));

        assertThatThrownBy(() -> service.audioUrl(3L))
            .isInstanceOf(OperationFailedException.class)
            .hasMessage("音乐目录不在数据目录下，无法试听");
        verify(localFileUrlPolicy, never()).sign(any());
    }

    @Test
    void audioUrlMissingFileThrowsNotFound() {
        when(musicMapper.selectById(3L)).thenReturn(row(3L, "不存在.mp3", "不存在", "1"));

        assertThatThrownBy(() -> service.audioUrl(3L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("音乐文件不存在");
    }

    // ===================== 对话链路兼容 =====================

    @Test
    void listAndSyncStillRegistersNewFilesForDialogueCaller() throws Exception {
        Files.write(musicDir.resolve("新文件.mp3"), "abc".getBytes());
        MusicDO existing = row(1L, "已有.mp3", "已有", "1");
        // 第一次 selectList 给 registerNewFiles 算已知文件名，第二次是最终返回的列表
        when(musicMapper.selectList(any())).thenReturn(List.of(), List.of(existing));
        when(musicMapper.selectOne(any())).thenReturn(null);

        List<MusicBO> all = service.listAndSync();

        assertThat(all).singleElement().extracting(MusicBO::getTitle).isEqualTo("已有");
        verify(musicMapper).insert(any(MusicDO.class));
    }

    // ===================== 夹具 =====================

    private static MusicDO row(Long musicId, String fileName, String title, String state) {
        MusicDO dataObject = new MusicDO();
        dataObject.setMusicId(musicId);
        dataObject.setFileName(fileName);
        dataObject.setTitle(title);
        dataObject.setState(state);
        dataObject.setPlayCount(0);
        return dataObject;
    }

    private static MusicPlayLogDO playLog(Long logId, String deviceId, Long musicId) {
        MusicPlayLogDO dataObject = new MusicPlayLogDO();
        dataObject.setLogId(logId);
        dataObject.setDeviceId(deviceId);
        dataObject.setMusicId(musicId);
        dataObject.setSongName("稻香.mp3");
        dataObject.setKeyword("播放稻香");
        dataObject.setCreateTime(LocalDateTime.of(2026, 10, 30, 12, 0, 0));
        return dataObject;
    }
}
