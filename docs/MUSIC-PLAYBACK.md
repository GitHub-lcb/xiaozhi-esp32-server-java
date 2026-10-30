# 音乐播放与设备 SD 卡

## 一、两条播放链路

| 链路 | 音频来源 | 谁负责 | 现状 |
|---|---|---|---|
| 服务端曲库 | 服务器 `/app/uploads/music/*.mp3` | `LocalMusicPlayer` + `Player.play(song, file)` | ✅ 已启用，流式下发 Opus |
| 设备 SD 卡 | 设备本地 SD 卡 | **固件**暴露 MCP 工具 | ⬜ 需固件实现，服务端无需改动 |

设备 SD 卡之所以不用服务端写代码：设备通过 MCP `tools/list` 上报自己有哪些工具，
`DeviceMcpService` 会自动把它们注册成 LLM 可调用的工具（见 `DeviceMcpService:191,252,281`）。
固件只要暴露一个工具，模型就能调用它播放本地音乐。

## 二、固件侧需要实现的工具（约定）

```json
{
  "name": "self.music.play_local",
  "description": "播放设备SD卡里的本地歌曲，适合无网络或需要离线播放的场景",
  "inputSchema": {
    "type": "object",
    "properties": {
      "song_name": { "type": "string", "description": "歌曲名称，可只给关键字" }
    },
    "required": ["song_name"]
  }
}
```

返回示例：`{"success": true, "song": "海阔天空"}` 或 `{"success": false, "reason": "SD 卡未插入"}`

命名注意：**不要**用 `play_music`，那是服务端曲库的工具名，重名会互相覆盖。

## 三、模型如何选择

- 用户说"放首歌" → 两个工具都在候选里，模型按描述选择
- 若设备工具描述里写明"离线可用/SD 卡"，模型会优先选它（本地播放不耗流量）
- 服务端工具找不到歌时会回一串可点歌曲名，模型据此回答用户

## 四、服务端曲库使用

曲库目录在宿主机是仓库根目录下的 `./music`，同时挂进 server 与 dialogue 两个容器：

```text
./music  ──bind──▶  /app/uploads/music   (server：后台曲库页上传 / 列表 / 删除 / 试听)
         ──bind──▶  /app/uploads/music   (dialogue：LocalMusicPlayer 点播)
```

- 后台管理台（智能体 → 曲库）上传、删除、上下架、同步目录都直接作用于这份目录，不需要再 `docker cp`
- 也可以手工把 mp3 放到宿主机的 `./music`（文件名建议 `歌手 - 歌名.mp3`），回后台点「同步目录」即可登记
- 「试听」返回的是带时效签名的 `/uploads/music/...` 地址，走的是 `LocalFileAccessInterceptor` 的签名校验，链接过期后重新点一次即可
- 曲库权限：`system:file:api:list / plays / update / sync / delete / preview / upload`，管理员角色默认拥有（升级脚本 `db/upgrade/2026-10-30__music_console.sql` 可给存量库补齐；后端这些端点同时声明了 `orRole="admin"` 兜底）
- 找不到歌时的可点列表来自曲库 DB（`sys_music`），新文件要先「同步目录」或打开一次曲库页才会登记

歌名匹配规则（`LocalMusicPlayer.resolveMusicFile`）：

1. 精确命中：`海阔天空` → `海阔天空.mp3`
2. 模糊命中：忽略大小写/空格/常见标点，包含关系即命中，`海阔天空` → `Beyond 海阔天空.mp3`
3. 多个候选取文件名最短的，避免命中"某歌现场版 2024 remaster"
4. 完全找不到时返回音乐库里前 8 首歌名，供模型解释
