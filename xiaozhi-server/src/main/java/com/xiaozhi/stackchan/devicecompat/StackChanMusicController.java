package com.xiaozhi.stackchan.devicecompat;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StackChan 设备侧内置音乐列表（Go 版 dance v1 {@code GET /musicList}）。
 * <p>
 * Go 实现是 {@code make([]string,1)} 再 append 一条，因此线上实际返回的是
 * {@code ["", "file/music/stackchan_music.mp3"]}（首元素为空串）。本接口保持同样输出，
 * 避免固件/App 按索引取元素时行为变化；路径与 Go 一致挂在 {@code /api/stackChan} 下。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 设备内置音乐", description = "固件侧获取内置背景音乐文件列表")
public class StackChanMusicController {

    /** 与 Go 当前线上行为逐元素一致（含首元素空串） */
    private static final List<String> BUILT_IN_MUSIC_LIST = List.of("", "file/music/stackchan_music.mp3");

    /**
     * Go 侧的 TokenAuthMiddleware 对缺失 token 是放行的，本处理器也不读 MAC，
     * 因此这里不强制 Device-Id，保持与 Go 一致的"公开静态列表"语义。
     */
    @SaIgnore
    @GetMapping("/musicList")
    @ResponseBody
    @Operation(summary = "内置音乐列表", description = "返回 Go 版同样的静态列表 [\"\", \"file/music/stackchan_music.mp3\"]")
    public StackChanApiResponse<List<String>> musicList() {
        return StackChanApiResponse.success(BUILT_IN_MUSIC_LIST);
    }
}
