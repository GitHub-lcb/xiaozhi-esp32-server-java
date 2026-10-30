package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.dialogue.runtime.Persona;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LocalMusicPlayer implements ToolsGlobalRegistry.GlobalFunction {
    public static final String TOOL_NAME = "play_music";
    private static final String MUSIC_SUFFIX = ".mp3";

    // 使用虚拟线程执行器处理定时任务
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("music-scheduler-", 0).factory());

    @Resource
    private SessionManager sessionManager;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Resource
    private com.xiaozhi.music.service.MusicLibraryService musicLibraryService;

    @Tool(name = TOOL_NAME, description = "音乐播放器,播放指定名称的歌曲", returnDirect = true)
    public String playMusic(@ToolParam(description = "要播放的歌曲名称") String songName, ToolContext toolContext) {
        String sessionId = (String) toolContext.getContext().get(Persona.TOOL_CONTEXT_SESSION_ID_KEY);
        ChatSession chatSession = sessionManager.getSession(sessionId);
        if (chatSession == null || chatSession.getPlayer() == null) {
            return "音乐播放失败";
        }
        if (songName == null || songName.isEmpty()) {
            return "你没有告诉我具体的歌曲名称，我播放不了！";
        }

        try {
            // 优先按曲库元数据匹配（别名/歌名/歌手），命中后按文件名定位磁盘文件
            com.xiaozhi.common.model.bo.MusicBO matched = musicLibraryService.match(songName);
            Path musicFile = matched == null ? resolveMusicFile(songName) : resolveMusicFile(matched.getFileName());
            if (musicFile == null) {
                var songs = listSongs();
                if (songs.isEmpty()) {
                    return "音乐库还是空的，暂时没有可以播放的歌曲";
                }
                return "我这里没有《" + songName + "》。可以播的有：" + String.join("、", songs.subList(0, Math.min(8, songs.size())));
            }
            String deviceId = chatSession.getDevice() == null ? null : chatSession.getDevice().getDeviceId();
            musicLibraryService.logPlay(deviceId, matched, songName);
            scheduler.schedule(() -> {
                // 必须异步处理，也就是先返回一个回应用户的字符串，再开始播放。
                chatSession.getPlayer().play(songName, musicFile);
            }, 60, TimeUnit.MILLISECONDS);
            return "尝试播放歌曲《" + songName + "》";

        } catch (Exception e) {
            log.error("device 音乐播放异常，song name: {}", songName, e);
            return "音乐播放失败";
        }
    }

    /**
     * 歌名解析到音乐目录下的 mp3 文件。歌名可能已带 .mp3（来自歌曲列表），
     * 解析结果必须仍在音乐目录内，文件不存在返回 null。
     */
    private Path resolveMusicFile(String songName) {
        Path musicDir = runtimePathConfig.resolveMusicDir();
        if (!Files.isDirectory(musicDir)) {
            return null;
        }
        String wanted = songName.trim();
        // 1) 精确命中（允许带或不带 .mp3）
        Path exact = musicDir.resolve(wanted.endsWith(MUSIC_SUFFIX) ? wanted : wanted + MUSIC_SUFFIX).normalize();
        if (exact.startsWith(musicDir) && Files.isRegularFile(exact)) {
            return exact;
        }
        // 2) 模糊命中：忽略大小写与空格，歌名包含关系即可（"海阔天空" 命中 "Beyond 海阔天空.mp3"）。
        //    多个候选时按文件名长度取最短的那个，避免"小星星"匹配到"小星星变奏曲现场版"。
        String key = normalizeForMatch(wanted);
        try (var stream = Files.list(musicDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().toLowerCase().endsWith(MUSIC_SUFFIX))
                    .filter(f -> normalizeForMatch(stripExtension(f.getFileName().toString())).contains(key))
                    .min(java.util.Comparator.comparingInt(f -> f.getFileName().toString().length()))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("扫描音乐目录失败: {}", musicDir, e);
            return null;
        }
    }

    private static String stripExtension(String name) {
        return name.toLowerCase().endsWith(MUSIC_SUFFIX) ? name.substring(0, name.length() - MUSIC_SUFFIX.length()) : name;
    }

    /** 去掉空格与常见标点，统一小写，用于宽松匹配 */
    private static String normalizeForMatch(String text) {
        return text.toLowerCase().replaceAll("[\\s\\-_·,，、。!！?？'\"\"]", "");
    }

    /** 音乐目录里可点的歌（用于"我这儿没有，但有这些"的回复） */
    private java.util.List<String> listSongs() {
        Path musicDir = runtimePathConfig.resolveMusicDir();
        if (!Files.isDirectory(musicDir)) {
            return java.util.List.of();
        }
        try (var stream = Files.list(musicDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(f -> f.getFileName().toString())
                    .filter(n -> n.toLowerCase().endsWith(MUSIC_SUFFIX))
                    .map(LocalMusicPlayer::stripExtension)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        ToolCallback[] tools = ToolCallbacks.from(this);
        return tools[0];
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getToolDescription() {
        return "播放音乐";
    }
}
