package com.xiaozhi.music;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;
import com.xiaozhi.common.model.bo.MusicStatsBO;
import com.xiaozhi.common.model.bo.MusicSyncBO;
import com.xiaozhi.music.convert.MusicConvert;
import com.xiaozhi.music.service.MusicLibraryService;
import com.xiaozhi.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 曲库 Web 层：分页/统计/编辑/上下架/同步/删除/试听签名，外加原有的上传白名单。
 * <p>
 * standalone MockMvc 不装 Sa-Token 拦截器，权限注解不会生效，所以另有一条反射用例逐个端点钉住
 * 「权限键 + orRole=admin」——这是线上整页 403 的修复点，漏一个注解在 standalone 测试里看不出来。
 */
class MusicControllerTest extends ControllerTestSupport {

    @TempDir
    Path musicDir;

    private MockMvc mockMvc;

    private MusicLibraryService musicLibraryService;

    @BeforeEach
    void setUp() {
        musicLibraryService = mock(MusicLibraryService.class);
        RuntimePathConfig runtimePathConfig = new RuntimePathConfig();
        runtimePathConfig.setMusicDir(musicDir.toString());
        MusicController controller = new MusicController();
        ReflectionTestUtils.setField(controller, "runtimePathConfig", runtimePathConfig);
        ReflectionTestUtils.setField(controller, "musicLibraryService", musicLibraryService);
        ReflectionTestUtils.setField(controller, "musicConvert", Mappers.getMapper(MusicConvert.class));
        mockMvc = buildMockMvc(controller);
    }

    // ===================== 上传（原契约保持不变） =====================

    @Test
    void uploadMusicRejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart("/api/file/music").file(mp3("song.mp3", new byte[0])))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("上传文件不能为空"));

        verifyNoInteractions(musicLibraryService);
    }

    @Test
    void uploadMusicRejectsDisallowedExtension() throws Exception {
        mockMvc.perform(multipart("/api/file/music").file(mp3("song.wav", "abc".getBytes())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("仅支持 .mp3 或 playlist.txt"));

        assertThat(Files.exists(musicDir.resolve("song.wav"))).isFalse();
        verify(musicLibraryService, never()).register(any());
    }

    @Test
    void uploadMusicStoresAllowedMp3File() throws Exception {
        mockMvc.perform(multipart("/api/file/music").file(mp3("song-test.mp3", "abc".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("song-test.mp3"));

        assertThat(musicDir.resolve("song-test.mp3")).hasBinaryContent("abc".getBytes());
        verify(musicLibraryService).register("song-test.mp3");
    }

    @Test
    void uploadMusicStoresPlaylistFile() throws Exception {
        mockMvc.perform(multipart("/api/file/music").file(mp3("playlist.txt", "song-test.mp3".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("playlist.txt"));

        assertThat(musicDir.resolve("playlist.txt")).hasBinaryContent("song-test.mp3".getBytes());
        verify(musicLibraryService).register("playlist.txt");
    }

    // ===================== 分页 / 统计 =====================

    @Test
    void listPassesFiltersToServiceAndFormatsTime() throws Exception {
        when(musicLibraryService.page(1, 10, "稻香", "1", "title", "asc"))
            .thenReturn(new PageResult<>(List.of(music()), 1L, 1, 10));

        mockMvc.perform(get("/api/file/music")
                .param("pageNo", "1")
                .param("pageSize", "10")
                .param("keyword", "稻香")
                .param("state", "1")
                .param("sortBy", "title")
                .param("sortOrder", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.pageNo").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(10))
            .andExpect(jsonPath("$.data.list[0].musicId").value(3))
            .andExpect(jsonPath("$.data.list[0].fileName").value("周杰伦 - 稻香.mp3"))
            .andExpect(jsonPath("$.data.list[0].title").value("稻香"))
            .andExpect(jsonPath("$.data.list[0].artist").value("周杰伦"))
            .andExpect(jsonPath("$.data.list[0].state").value("1"))
            .andExpect(jsonPath("$.data.list[0].createTime").value("2026-10-30 12:00:00"))
            .andExpect(jsonPath("$.data.list[0].updateTime").value("2026-10-30 12:05:00"));
    }

    @Test
    void listUsesBasePageDefaultsWhenNoQueryParams() throws Exception {
        when(musicLibraryService.page(1, 10, null, null, null, null))
            .thenReturn(new PageResult<>(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/api/file/music"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void listAcceptsEmptyPagingParams() throws Exception {
        // 前端清空输入框会发 pageNo=&pageSize=，绑定结果是 null；这里只保证不因拆箱 NPE 变成 500，
        // 默认值兜底在 Service 的 normalizePageNo/normalizePageSize 里（见 MusicLibraryServiceImplTest）
        when(musicLibraryService.page(null, null, null, null, null, null))
            .thenReturn(new PageResult<>(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/api/file/music").param("pageNo", "").param("pageSize", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pageNo").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void listRejectsPageSizeOverBaseLimit() throws Exception {
        mockMvc.perform(get("/api/file/music").param("pageSize", "1001"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(musicLibraryService);
    }

    @Test
    void statsReturnsAggregates() throws Exception {
        MusicStatsBO stats = new MusicStatsBO();
        stats.setTotal(10L);
        stats.setEnabled(8L);
        stats.setDisabled(2L);
        stats.setTotalPlayCount(33L);
        stats.setTotalDurationSec(2400L);
        stats.setTotalSizeBytes(104857600L);
        stats.setTodayPlayCount(5L);
        when(musicLibraryService.stats()).thenReturn(stats);

        mockMvc.perform(get("/api/file/music/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(10))
            .andExpect(jsonPath("$.data.enabled").value(8))
            .andExpect(jsonPath("$.data.disabled").value(2))
            .andExpect(jsonPath("$.data.totalPlayCount").value(33))
            .andExpect(jsonPath("$.data.totalDurationSec").value(2400))
            .andExpect(jsonPath("$.data.totalSizeBytes").value(104857600))
            .andExpect(jsonPath("$.data.todayPlayCount").value(5));
    }

    // ===================== 点播记录 =====================

    @Test
    void playsReturnsDeviceAndMusicDisplayNames() throws Exception {
        MusicPlayLogBO row = new MusicPlayLogBO();
        row.setLogId(100L);
        row.setDeviceId("aa:bb:cc");
        row.setDeviceName("客厅音箱");
        row.setMusicId(3L);
        row.setSongName("稻香.mp3");
        row.setMusicTitle("稻香");
        row.setKeyword("播放稻香");
        row.setCreateTime(LocalDateTime.of(2026, 10, 30, 12, 0, 0));
        when(musicLibraryService.pagePlays(1, 10, "稻香", "aa:bb:cc"))
            .thenReturn(new PageResult<>(List.of(row), 1L, 1, 10));

        mockMvc.perform(get("/api/file/music/plays")
                .param("pageNo", "1")
                .param("pageSize", "10")
                .param("keyword", "稻香")
                .param("deviceId", "aa:bb:cc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].deviceName").value("客厅音箱"))
            .andExpect(jsonPath("$.data.list[0].musicTitle").value("稻香"))
            .andExpect(jsonPath("$.data.list[0].createTime").value("2026-10-30 12:00:00"));
    }

    // ===================== 编辑 / 上下架 =====================

    @Test
    void updateConvertsBodyToPatchAndReturnsUpdatedRow() throws Exception {
        MusicBO updated = music();
        updated.setState("0");
        updated.setTitle("稻香（改名）");
        when(musicLibraryService.update(eq(3L), any(MusicBO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/file/music/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "title", "稻香（改名）",
                    "artist", "周杰伦",
                    "aliases", "稻香,周杰伦",
                    "state", "0"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("稻香（改名）"))
            .andExpect(jsonPath("$.data.state").value("0"));

        org.mockito.ArgumentCaptor<MusicBO> captor = org.mockito.ArgumentCaptor.forClass(MusicBO.class);
        verify(musicLibraryService).update(eq(3L), captor.capture());
        MusicBO patch = captor.getValue();
        assertThat(patch.getTitle()).isEqualTo("稻香（改名）");
        assertThat(patch.getArtist()).isEqualTo("周杰伦");
        assertThat(patch.getAliases()).isEqualTo("稻香,周杰伦");
        assertThat(patch.getState()).isEqualTo("0");
        // 请求体没带的列必须留在 null，Service 据此判断「不修改」
        assertThat(patch.getFileName()).isNull();
        assertThat(patch.getPlayCount()).isNull();
    }

    @Test
    void updateRejectsBlankTitle() throws Exception {
        mockMvc.perform(put("/api/file/music/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("title", "  "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("歌名不能为空"));

        verifyNoInteractions(musicLibraryService);
    }

    @Test
    void updateRejectsUnknownState() throws Exception {
        mockMvc.perform(put("/api/file/music/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("title", "稻香", "state", "2"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("状态只能是 1（上架）或 0（下架）"));

        verifyNoInteractions(musicLibraryService);
    }

    @Test
    void updateMissingMusicReturns404() throws Exception {
        when(musicLibraryService.update(eq(99L), any(MusicBO.class)))
            .thenThrow(new ResourceNotFoundException("曲目不存在"));

        mockMvc.perform(put("/api/file/music/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("title", "稻香"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("曲目不存在"));
    }

    // ===================== 删除 / 同步 =====================

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/file/music/3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(musicLibraryService).delete(3L);
    }

    @Test
    void deleteMissingMusicReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("曲目不存在")).when(musicLibraryService).delete(99L);

        mockMvc.perform(delete("/api/file/music/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("曲目不存在"));
    }

    @Test
    void syncReturnsAddedRemovedAndTotal() throws Exception {
        when(musicLibraryService.sync()).thenReturn(new MusicSyncBO(2, 1, 10L));

        mockMvc.perform(post("/api/file/music/sync"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.added").value(2))
            .andExpect(jsonPath("$.data.removed").value(1))
            .andExpect(jsonPath("$.data.total").value(10));
    }

    // ===================== 试听 =====================

    @Test
    void audioUrlReturnsSignedUrl() throws Exception {
        when(musicLibraryService.audioUrl(3L)).thenReturn("uploads/music/稻香.mp3?exp=1&sig=abc");

        mockMvc.perform(get("/api/file/music/3/audio-url"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.url").value("uploads/music/稻香.mp3?exp=1&sig=abc"));
    }

    @Test
    void audioUrlMissingFileReturns404() throws Exception {
        when(musicLibraryService.audioUrl(99L)).thenThrow(new ResourceNotFoundException("音乐文件不存在"));

        mockMvc.perform(get("/api/file/music/99/audio-url"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("音乐文件不存在"));
    }

    // ===================== 权限注解 =====================

    @Test
    void everyEndpointDeclaresPermissionKeyAndAdminFallback() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("list", "system:file:api:list");
        expected.put("stats", "system:file:api:list");
        expected.put("plays", "system:file:api:plays");
        expected.put("update", "system:file:api:update");
        expected.put("delete", "system:file:api:delete");
        expected.put("sync", "system:file:api:sync");
        expected.put("audioUrl", "system:file:api:preview");
        expected.put("uploadMusic", "system:file:api:upload");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Method method = Arrays.stream(MusicController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(entry.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("找不到端点方法 " + entry.getKey()));
            SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
            assertThat(annotation)
                .as("%s 缺少 @SaCheckPermission，端点会对所有角色恒定 403", entry.getKey())
                .isNotNull();
            assertThat(annotation.value())
                .as("%s 的权限键必须与契约 §2 一致", entry.getKey())
                .contains(entry.getValue());
            assertThat(annotation.orRole())
                .as("%s 缺少 orRole=admin，授权行没补齐时管理员也会 403", entry.getKey())
                .contains("admin");
        }
    }

    // ===================== 夹具 =====================

    private static MusicBO music() {
        MusicBO bo = new MusicBO();
        bo.setMusicId(3L);
        bo.setFileName("周杰伦 - 稻香.mp3");
        bo.setTitle("稻香");
        bo.setArtist("周杰伦");
        bo.setAliases("稻香,周杰伦");
        bo.setDurationSec(223);
        bo.setSizeBytes(3670016L);
        bo.setPlayCount(12);
        bo.setState("1");
        bo.setCreateTime(LocalDateTime.of(2026, 10, 30, 12, 0, 0));
        bo.setUpdateTime(LocalDateTime.of(2026, 10, 30, 12, 5, 0));
        return bo;
    }

    private static MockMultipartFile mp3(String fileName, byte[] content) {
        return new MockMultipartFile("file", fileName, "audio/mpeg", content);
    }
}
