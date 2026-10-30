package com.xiaozhi.music.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.common.model.bo.MusicBO;
import com.xiaozhi.common.model.bo.MusicPlayLogBO;
import com.xiaozhi.music.dal.mysql.dataobject.MusicDO;
import com.xiaozhi.music.dal.mysql.dataobject.MusicPlayLogDO;
import com.xiaozhi.music.dal.mysql.mapper.MusicMapper;
import com.xiaozhi.music.dal.mysql.mapper.MusicPlayLogMapper;
import com.xiaozhi.music.service.MusicLibraryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {

    private static final String SUFFIX = ".mp3";
    private static final String STATE_ON = "1";
    private static final String STATE_OFF = "0";

    @Resource
    private MusicMapper musicMapper;

    @Resource
    private MusicPlayLogMapper musicPlayLogMapper;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MusicBO> listAndSync() {
        Path dir = musicDir();
        List<MusicDO> existing = musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery());
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(SUFFIX))
                        .map(f -> f.getFileName().toString())
                        .filter(name -> existing.stream().noneMatch(e -> e.getFileName().equals(name)))
                        .forEach(this::register);
            } catch (Exception e) {
                log.warn("扫描音乐目录失败: {}", dir, e);
            }
        }
        return musicMapper.selectList(Wrappers.<MusicDO>lambdaQuery().orderByAsc(MusicDO::getTitle)).stream()
                .map(this::toBO)
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
                .map(this::toBO)
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
        Path file = musicDir().resolve(fileName).normalize();
        MusicDO dataObject = musicMapper.selectOne(Wrappers.<MusicDO>lambdaQuery().eq(MusicDO::getFileName, fileName));
        if (dataObject == null) {
            dataObject = new MusicDO();
            dataObject.setFileName(fileName);
            dataObject.setState(STATE_ON);
            dataObject.setPlayCount(0);
        }
        dataObject.setSizeBytes(Files.isRegularFile(file) ? file.toFile().length() : null);
        dataObject.setDurationSec(probeDuration(file));
        fillTitleAndArtist(dataObject);
        if (dataObject.getMusicId() == null) {
            musicMapper.insert(dataObject);
        } else {
            dataObject.setState(STATE_ON);
            musicMapper.updateById(dataObject);
        }
        return toBO(dataObject);
    }

    @Override
    public void remove(String fileName) {
        MusicDO dataObject = musicMapper.selectOne(Wrappers.<MusicDO>lambdaQuery().eq(MusicDO::getFileName, fileName));
        if (dataObject == null) {
            return;
        }
        dataObject.setState(STATE_OFF);
        musicMapper.updateById(dataObject);
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
                .map(row -> {
                    MusicPlayLogBO bo = new MusicPlayLogBO();
                    bo.setLogId(row.getLogId());
                    bo.setDeviceId(row.getDeviceId());
                    bo.setMusicId(row.getMusicId());
                    bo.setKeyword(row.getKeyword());
                    bo.setSongName(row.getSongName());
                    bo.setFinished(row.getFinished());
                    bo.setCreateTime(row.getCreateTime());
                    return bo;
                })
                .toList();
    }

    private Path musicDir() {
        return Path.of(runtimePathConfig.getMusicDir()).toAbsolutePath().normalize();
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
        if (!Files.isRegularFile(file)) {
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

    private MusicBO toBO(MusicDO dataObject) {
        MusicBO bo = new MusicBO();
        bo.setMusicId(dataObject.getMusicId());
        bo.setFileName(dataObject.getFileName());
        bo.setTitle(dataObject.getTitle());
        bo.setArtist(dataObject.getArtist());
        bo.setAliases(dataObject.getAliases());
        bo.setDurationSec(dataObject.getDurationSec());
        bo.setSizeBytes(dataObject.getSizeBytes());
        bo.setPlayCount(dataObject.getPlayCount());
        bo.setState(dataObject.getState());
        bo.setCreateTime(dataObject.getCreateTime());
        return bo;
    }
}
