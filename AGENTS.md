# AGENTS.md — 维护约定

本文件给后续接手的 AI 代理与维护者看，约束怎么改、怎么发。

## 发布流程（必须遵守）

1. **不要每次改完就提交/打 tag/部署**。攒一批改动，列清单给用户确认后再提交。
2. 用户说「提交」→ 只 commit + push，不打 tag。
3. 用户说「发版」→ 打 tag：`git tag -a vX.Y.Z -m "..."` 并 push，等 CI 构建镜像。
4. 用户说「部署」→ 在服务器执行 `./deploy.sh X.Y.Z`（不要手工 sed/pull/up）。
5. 详细步骤见 `docs/DEPLOY.md`。

## 数据库变更

- 结构变更必须**同时**改 `db/init.sql`（新装）和 `db/upgrade/<日期>__<说明>.sql`（升级）。
- 升级脚本必须幂等：`CREATE TABLE IF NOT EXISTS`、`INSERT IGNORE`、加列不删列、不 DROP 业务表。
- `sys_auth_role_permission` 只有 `(id, authRoleId, permissionId, createTime)` 四列，别塞 `updateTime`。
- 新增 `@SaCheckPermission` 的 key 必须先写进 `db/init.sql`，否则 `PermissionKeyContractArchTest` 会红。

## 改了权限/角色之后

权限按角色缓存在 Redis（`XiaoZhi:Permission*`，TTL 7 天）。
改完数据库必须清缓存，或让用户去后台重存一次角色权限，否则接口 403。

## 架构守卫（提交前至少跑一次）

```bash
mvn -B -o -pl xiaozhi-server -am test -Dtest='*ArchTest' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
cd web && npx vue-tsc --build --force
```

常见会红的点：
- 新增包没加进两个启动类的 `@ComponentScan` / `@MapperScan`（`ComponentScanCoverageArchTest`）
- Controller 直接返回 `Map` / `BO`（`ServiceLayerArchTest`）→ 用 `ApiResponse<Object>` 或专用 Resp
- server 模块 import 了 dal 或第三方 SDK（`ModuleBoundaryArchTest`）
- 时间字段命名：模型里只能用 `xxxTime`，不能 `xxxAt`（`TimeSourceArchTest`）

## 对话链路注意事项

- 角色属性（音色/模型/VAD）变更靠 Redis 广播 `xiaozhi:role-updated` 触发 `session.setPersona(null)`；
  **Persona 为空时必须重建**（`ensurePersona`），直接丢弃本轮会让用户以为"设备坏了"。
- TTS 音频流式入口是 `StreamingTtsService.synthesizeStream`；非流式 provider 由
  `FileBackedStreamingAdapter` 包装，分块必须与旧路径一致。
- 播放器节拍（60ms/帧）与打断逻辑不要动，改音频来源即可。

## 设备侧

- OTA 响应里带 `mqtt` 块的条件是 `XIAOZHI_MQTT_ENABLED=true`。
  **固件不支持 MQTT 时必须关**，否则设备会切到 MQTT 通道后失联。
- 设备 MCP 工具（`tools/list`）会被自动注册成 LLM 可调用工具，服务端无需为设备本地能力写代码。
  命名避免与内置工具重名（如 `play_music` 已被服务端曲库占用）。
