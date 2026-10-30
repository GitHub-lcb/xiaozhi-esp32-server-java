package com.xiaozhi.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.xiaozhi.music.service.MusicLibraryService;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {

    private static final String SUFFIX = ".mp3";
    private static final String STATE_ON = "1";
    private static final String STATE_OFF = "0";

    /** 契约规定单页上限 100，BasePageReq 允许到 1000，这里按曲库页的实际需要收窄 */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Resource
    private MusicMapper musicMapper;

    @Resource
    private MusicPlayLogMapper musicPlayLogMapper;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Resource
    private DeviceService deviceService;

    @Resource
    private LocalFileUrlPolicy localFileUrlPolicy;

    @Resource
    private MusicConvert musicConvert;

    // ===================== 对话链路 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MusicBO> listAndSync() {
        registerNewFiles();
        return musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery().orderByAsc(MusicDO::getTitle)).stream()
                .map(musicConvert::toBO)
                .toList();
    }

    @Override
    public MusicBO match(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        List<MusicDO> all = musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery().eq(MusicDO::getState, STATE_ON));
        String key = normalize(keyword);
        // 1) 别名精确 2) 文件名/标题精确 3) 包含；同级取标题最短的，避免命中"某某现场版"
        return all.stream()
                .filter(m -> matchesExactly(m, key))
                .findFirst()
                .or(() -> all.stream().filter(m -> matchesContain(m, key))
                        .min(java.util.Comparator.comparingInt(m -> m.getTitle() == null ? 99 : m.getTitle().length())))
                .map(musicConvert::toBO)
                .orElse(null);
    }

    private boolean matchesExactly(MusicDO music, String key) {
        return equalsKey(music.getTitle(), key) || equalsKey(music.getFileName(), key)
                || (music.getAliases() != null && java.util.Arrays.stream(music.getAliases().split(","))
                        .anyMatch(alias -> equalsKey(alias, key)));
    }

    private boolean matchesContain(MusicDO music, String key) {
        return containsKey(music.getTitle(), key) || containsKey(music.getFileName(), key)
                || (music.getAliases() != null && java.util.Arrays.stream(music.getAliases().split(","))
                        .anyMatch(alias -> containsKey(alias, key)));
    }

    private static boolean equalsKey(String value, String key) {
        return value != null && normalize(value).equals(key);
    }

    private static boolean containsKey(String value, String key) {
        return value != null && normalize(value).contains(key);
    }

    /** 去掉空格、常见标点与扩展名后统一小写，用于宽松匹配 */
    static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(SUFFIX)) {
            lower = lower.substring(0, lower.length() - SUFFIX.length());
        }
        return lower.replaceAll("[\\s\\-_·,，、。!！?？'\"\"]", "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicBO register(String fileName) {
        Path file = musicFile(fileName);
        MusicDO dataObject = musicMapper.selectOne(Wrappers.<MusicDO>lambdaQuery().eq(MusicDO::getFileName, fileName));
        boolean insert = dataObject == null;
        if (insert) {
            dataObject = new MusicDO();
            dataObject.setFileName(fileName);
            dataObject.setState(STATE_ON);
            dataObject.setPlayCount(0);
            fillTitleAndArtist(dataObject);
        } else if (!StringUtils.hasText(dataObject.getTitle())) {
            // 老数据没拆出歌名时补一次；已有歌名说明可能被人工改过，重新登记不能拿文件名覆盖
            fillTitleAndArtist(dataObject);
        }
        dataObject.setSizeBytes(file != null && Files.isRegularFile(file) ? file.toFile().length() : null);
        dataObject.setDurationSec(probeDuration(file));
        if (insert) {
            musicMapper.insert(dataObject);
        } else {
            // 重新上传/补登记都会走到这里：文件已回来说明要重新上架
            dataObject.setState(STATE_ON);
            musicMapper.updateById(dataObject);
        }
        return musicConvert.toBO(dataObject);
    }

    @Override
    public void remove(String fileName) {
        MusicDO dataObject = musicMapper.selectOne(Wrappers.<MusicDO>lambdaQuery().eq(MusicDO::getFileName, fileName));
        if (dataObject == null) {
            return;
        }
        MusicDO update = new MusicDO();
        update.setMusicId(dataObject.getMusicId());
        update.setState(STATE_OFF);
        musicMapper.updateById(update);
    }

    @Override
    public void logPlay(String deviceId, MusicBO music, String keyword) {
        try {
            MusicPlayLogDO dataObject = new MusicPlayLogDO();
            dataObject.setDeviceId(deviceId);
            dataObject.setMusicId(music == null ? null : music.getMusicId());
            dataObject.setSongName(music == null ? keyword : music.getFileName());
            dataObject.setKeyword(keyword);
            dataObject.setFinished("0");
            musicPlayLogMapper.insert(dataObject);
            if (music != null && music.getMusicId() != null) {
                var update = new MusicDO();
                update.setMusicId(music.getMusicId());
                update.setPlayCount((music.getPlayCount() == null ? 0 : music.getPlayCount()) + 1);
                musicMapper.updateById(update);
            }
        } catch (RuntimeException e) {
            log.warn("记录点播失败 deviceId={} keyword={}", deviceId, keyword, e);
        }
    }

    @Override
    public List<MusicPlayLogBO> recentPlays(int limit) {
        int size = limit <= 0 ? 20 : Math.min(limit, 100);
        return musicPlayLogMapper.selectList(Wrappers.<MusicPlayLogDO>lambdaQuery()
                        .orderByDesc(MusicPlayLogDO::getLogId).last("limit " + size)).stream()
                .map(musicConvert::toBO)
                .toList();
    }

    // ===================== 后台曲库页 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<MusicBO> page(Integer pageNo, Integer pageSize, String keyword, String state,
                                    String sortBy, String sortOrder) {
        // 轻量同步：手工 scp 进目录的歌打开列表就能看到；文件缺失不在这里处理，避免"看一次列表就触发大规模下架"
        registerNewFiles();
        int current = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        LambdaQueryWrapper<MusicDO> wrapper = Wrappers.<MusicDO>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            wrapper.and(w -> w.like(MusicDO::getTitle, key)
                    .or().like(MusicDO::getArtist, key)
                    .or().like(MusicDO::getAliases, key)
                    .or().like(MusicDO::getFileName, key));
        }
        if (STATE_ON.equals(state) || STATE_OFF.equals(state)) {
            wrapper.eq(MusicDO::getState, state);
        }
        applySort(wrapper, sortBy, sortOrder);
        IPage<MusicDO> result = musicMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(result.getRecords().stream().map(musicConvert::toBO).toList(),
                result.getTotal(), current, size);
    }

    /** 排序字段走白名单：sortBy 是前端可控字符串，直接拼进 order by 就是注入面 */
    private void applySort(LambdaQueryWrapper<MusicDO> wrapper, String sortBy, String sortOrder) {
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        SFunction<MusicDO, ?> column = switch (sortBy == null ? "" : sortBy.trim()) {
            case "playCount" -> MusicDO::getPlayCount;
            case "createTime" -> MusicDO::getCreateTime;
            case "durationSec" -> MusicDO::getDurationSec;
            default -> MusicDO::getTitle;
        };
        wrapper.orderBy(true, desc, column);
        // 排序键可能重复（同名歌、点播次数相同），补主键让翻页稳定，同一首不会在两页都出现
        wrapper.orderByAsc(MusicDO::getMusicId);
    }

    @Override
    public MusicStatsBO stats() {
        List<MusicDO> rows = musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery());
        // 曲库是人工维护的小表，一次全量扫描比维护多条 SUM/COUNT SQL 更不容易让统计口径漂移
        MusicStatsBO stats = new MusicStatsBO();
        long enabled = rows.stream().filter(row -> STATE_ON.equals(row.getState())).count();
        stats.setTotal((long) rows.size());
        stats.setEnabled(enabled);
        stats.setDisabled(rows.size() - enabled);
        stats.setTotalPlayCount(rows.stream().mapToLong(row -> valueOrZero(row.getPlayCount())).sum());
        stats.setTotalDurationSec(rows.stream().mapToLong(row -> valueOrZero(row.getDurationSec())).sum());
        stats.setTotalSizeBytes(rows.stream().mapToLong(row -> valueOrZero(row.getSizeBytes())).sum());
        Long todayPlayCount = musicPlayLogMapper.selectCount(Wrappers.<MusicPlayLogDO>lambdaQuery()
                .ge(MusicPlayLogDO::getCreateTime, DateUtils.today().atStartOfDay()));
        stats.setTodayPlayCount(todayPlayCount == null ? 0L : todayPlayCount);
        return stats;
    }

    @Override
    public PageResult<MusicPlayLogBO> pagePlays(Integer pageNo, Integer pageSize, String keyword, String deviceId) {
        int current = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        LambdaQueryWrapper<MusicPlayLogDO> wrapper = Wrappers.<MusicPlayLogDO>lambdaQuery()
                .orderByDesc(MusicPlayLogDO::getLogId);
        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            wrapper.and(w -> w.like(MusicPlayLogDO::getKeyword, key).or().like(MusicPlayLogDO::getSongName, key));
        }
        if (StringUtils.hasText(deviceId)) {
            wrapper.eq(MusicPlayLogDO::getDeviceId, deviceId.trim());
        }
        IPage<MusicPlayLogDO> result = musicPlayLogMapper.selectPage(new Page<>(current, size), wrapper);
        List<MusicPlayLogBO> list = result.getRecords().stream().map(musicConvert::toBO).toList();
        fillDisplayNames(list);
        return new PageResult<>(list, result.getTotal(), current, size);
    }

    /**
     * 设备名与歌名不在点播表里冗余存：设备改名、歌名被编辑后列表要跟着变，
     * 所以分页拿到的 id 再各批量查一次，查不到的留 null（设备解绑/曲目行已不存在）。
     */
    private void fillDisplayNames(List<MusicPlayLogBO> list) {
        if (list.isEmpty()) {
            return;
        }
        List<String> deviceIds = list.stream()
                .map(MusicPlayLogBO::getDeviceId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (!deviceIds.isEmpty()) {
            // 用 HashMap 而不是 Collectors.toMap：设备名允许为 null，toMap 遇到 null value 会直接抛 NPE
            Map<String, String> deviceNames = new HashMap<>();
            for (DeviceBO device : deviceService.listByDeviceIds(deviceIds)) {
                if (StringUtils.hasText(device.getDeviceId())) {
                    deviceNames.put(device.getDeviceId(), device.getDeviceName());
                }
            }
            list.forEach(row -> row.setDeviceName(deviceNames.get(row.getDeviceId())));
        }
        List<Long> musicIds = list.stream()
                .map(MusicPlayLogBO::getMusicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!musicIds.isEmpty()) {
            Map<Long, String> musicTitles = new HashMap<>();
            for (MusicDO music : musicMapper.selectList(
                    Wrappers.<MusicDO>lambdaQuery().in(MusicDO::getMusicId, musicIds))) {
                musicTitles.put(music.getMusicId(), music.getTitle());
            }
            list.forEach(row -> row.setMusicTitle(musicTitles.get(row.getMusicId())));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicBO update(Long musicId, MusicBO patch) {
        MusicDO dataObject = requireMusic(musicId);
        if (patch != null) {
            if (StringUtils.hasText(patch.getTitle())) {
                dataObject.setTitle(patch.getTitle().trim());
            }
            if (patch.getArtist() != null) {
                dataObject.setArtist(patch.getArtist().trim());
            }
            if (patch.getAliases() != null) {
                dataObject.setAliases(patch.getAliases().trim());
            }
            if (STATE_ON.equals(patch.getState()) || STATE_OFF.equals(patch.getState())) {
                dataObject.setState(patch.getState());
            }
        }
        // BaseDO 的 updateTime 自动填充只在字段为 null 时生效，这里显式取一次应用时钟，
        // 返回给前端的才是这次修改的真实更新时间（也符合时间列只走 DateUtils 的约定）
        dataObject.setUpdateTime(DateUtils.now());
        musicMapper.updateById(dataObject);
        return musicConvert.toBO(dataObject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long musicId) {
        MusicDO dataObject = requireMusic(musicId);
        Path file = musicFile(dataObject.getFileName());
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new OperationFailedException("删除音乐文件失败，请稍后重试", e);
            }
        }
        // 只下架不物理删行：点播历史与累计统计都挂在 musicId 上，删行会让历史变成查不到歌名的孤儿记录
        MusicDO update = new MusicDO();
        update.setMusicId(musicId);
        update.setState(STATE_OFF);
        musicMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicSyncBO sync() {
        Path dir = musicDir();
        List<MusicDO> rows = musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery());
        if (!Files.isDirectory(dir)) {
            // 目录不可读时宁可什么都不做：把全库下架会把一次配置错误放大成数据事故
            log.warn("音乐目录不存在或不可读，跳过同步: {}", dir);
            return new MusicSyncBO(0, 0, countAll());
        }
        Set<String> onDisk = listMp3FileNames(dir);
        Set<String> known = rows.stream().map(MusicDO::getFileName).collect(Collectors.toSet());
        int added = 0;
        for (String fileName : onDisk) {
            if (!known.contains(fileName)) {
                register(fileName);
                added++;
            }
        }
        int removed = 0;
        for (MusicDO row : rows) {
            if (!STATE_ON.equals(row.getState())) {
                continue;
            }
            Path file = musicFile(row.getFileName());
            if (file != null && Files.isRegularFile(file)) {
                continue;
            }
            MusicDO update = new MusicDO();
            update.setMusicId(row.getMusicId());
            update.setState(STATE_OFF);
            musicMapper.updateById(update);
            removed++;
        }
        return new MusicSyncBO(added, removed, countAll());
    }

    @Override
    public String audioUrl(Long musicId) {
        MusicDO dataObject = requireMusic(musicId);
        Path dir = musicDir();
        Path file = musicFile(dataObject.getFileName());
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("音乐文件不存在");
        }
        Path dataDir = runtimePathConfig.resolveDataDir();
        if (!dir.startsWith(dataDir)) {
            throw new OperationFailedException("音乐目录不在数据目录下，无法试听");
        }
        // 签名只认相对 dataDir 的存储键：绝对路径既签不上名，也会把部署路径暴露给前端
        return localFileUrlPolicy.sign(dataDir.relativize(file).toString().replace('\\', '/'));
    }

    // ===================== 内部工具 =====================

    /** 轻量同步：只把目录里新出现的 mp3 补登记，不处理文件缺失（那是显式 sync 的职责） */
    private void registerNewFiles() {
        Path dir = musicDir();
        if (!Files.isDirectory(dir)) {
            return;
        }
        Set<String> known = musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery()).stream()
                .map(MusicDO::getFileName)
                .collect(Collectors.toSet());
        for (String fileName : listMp3FileNames(dir)) {
            if (!known.contains(fileName)) {
                register(fileName);
            }
        }
    }

    /** 音乐目录下的 mp3 文件名；保持目录顺序，便于排查与断言 */
    private Set<String> listMp3FileNames(Path dir) {
        if (!Files.isDirectory(dir)) {
            return Set.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(SUFFIX))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            log.warn("扫描音乐目录失败: {}", dir, e);
            return Set.of();
        }
    }

    private MusicDO requireMusic(Long musicId) {
        MusicDO dataObject = musicId == null ? null : musicMapper.selectById(musicId);
        if (dataObject == null) {
            throw new ResourceNotFoundException("曲目不存在");
        }
        return dataObject;
    }

    /** 文件名解析成音乐目录下的绝对路径；含目录穿越或越出音乐目录时返回 null */
    private Path musicFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        Path dir = musicDir();
        Path file = dir.resolve(Paths.get(fileName).getFileName().toString()).normalize();
        return file.startsWith(dir) ? file : null;
    }

    private Path musicDir() {
        return Path.of(runtimePathConfig.getMusicDir()).toAbsolutePath().normalize();
    }

    private long countAll() {
        Long total = musicMapper.selectCount(Wrappers.<MusicDO>lambdaQuery());
        return total == null ? 0L : total;
    }

    /** 空串查询参数会绑定成 null，这里兜底成默认值，避免控制器拆箱 NPE 变成 500 */
    private static int normalizePageNo(Integer pageNo) {
        return pageNo == null ? 1 : Math.max(pageNo, 1);
    }

    private static int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static long valueOrZero(Number value) {
        return value == null ? 0L : value.longValue();
    }

    /** 文件名形如 "周杰伦 - 稻香.mp3" 时拆出歌手与歌名，否则整个文件名当歌名 */
    private void fillTitleAndArtist(MusicDO dataObject) {
        String base = dataObject.getFileName();
        base = base.toLowerCase(Locale.ROOT).endsWith(SUFFIX)
                ? base.substring(0, base.length() - SUFFIX.length()) : base;
        String[] parts = base.split("\\s*[-—]\\s*", 2);
        if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1])) {
            dataObject.setArtist(parts[0].trim());
            dataObject.setTitle(parts[1].trim());
        } else {
            dataObject.setTitle(base.trim());
        }
        if (!StringUtils.hasText(dataObject.getAliases())) {
            dataObject.setAliases(dataObject.getTitle());
        }
    }

    /** 读 mp3 帧头估算时长；解析失败不阻塞上传 */
    private Integer probeDuration(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        long millis = 0;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            var bitstream = new javazoom.jl.decoder.Bitstream(in);
            javazoom.jl.decoder.Header header;
            while ((header = bitstream.readFrame()) != null) {
                millis += (long) header.ms_per_frame();
                bitstream.closeFrame();
            }
            bitstream.close();
        } catch (Exception e) {
            log.debug("解析 mp3 时长失败: {}", file, e);
            return null;
        }
        return millis <= 0 ? null : (int) (millis / 1000);
    }
}
