# StackChan 整机接入说明（自建 Java 后端）

> 本文记录 M5Stack StackChan 开源整机从 Go 版后端迁移到本仓库后的接口、配置、验证方式和当前边界。
> 涉及三个仓库：本仓库（Java 后端）、`StackChan` 仓库的 `firmware/`（主控固件）与 `app/`（Flutter App）。

## 1. 架构

```
StackChan 固件 ──OTA / 设备 REST──▶ server:8091   /api/device/ota、/api/stackChan/**
       │                              └─ MySQL：sys_device/sys_user/sys_role + sys_stackchan_*
       ├──AI 语音 WebSocket────────▶ dialogue:8092 /ws/xiaozhi/v1/
       └──伴侣 WebSocket───────────▶ server:8091   /api/stackChan/ws
Flutter App ───REST────────────────▶ server:8091   /api/stackChan/v2/**
Flutter App ───伴侣 WebSocket──────▶ server:8091   /api/stackChan/ws
```

- 设备首次 OTA 自动建档：`board.type` 含 `stack-chan`/`stackchan` 且设备不存在时，自动绑定到
  `xiaozhi.stackchan.owner-user-id`（默认 admin）的默认角色，不再走官方激活码流程。
- 设备身份：小写冒号 MAC（`sys_device.deviceId`）；App 侧出入参统一转 12 位小写纯 hex。
- 设备鉴权：`Device-Id` + HMAC token（与 xiaozhi WS 同一套 `xiaozhi.device-auth.secret`）；
  App 鉴权：`token` 头（或 `Authorization: Bearer`）里的 Sa-Token，伴侣 WS 还需校验设备归属。

## 2. 环境变量 / 配置

```ini
# Java 端（server 与 dialogue 同值）
XIAOZHI_DEVICE_AUTH_SECRET=<与固件 CONFIG_STACKCHAN_DEVICE_AUTH_SECRET 相同>
XIAOZHI_SERVER_DOMAIN=<你的域名>
XIAOZHI_STACKCHAN_AUTO_PROVISION=true
XIAOZHI_STACKCHAN_OWNER_USER_ID=1
```

固件（`StackChan/firmware/sdkconfig.defaults.local`）：

```ini
CONFIG_STACKCHAN_SERVER_URL="https://<domain>/api"
CONFIG_STACKCHAN_DEVICE_AUTH_SECRET="<同上>"
CONFIG_OTA_URL="https://<domain>/api/device/ota"
```

App 构建参数：

```bash
flutter build apk --release \
  --dart-define=STACKCHAN_HOST=<domain> \
  --dart-define=STACKCHAN_SCHEME=https \
  --dart-define=STACKCHAN_BLE_PRIVATE_KEY="$(cat keys/ble_private_key.pem)"
```

前置条件：后台必须先配置 LLM 和至少一个**默认角色**（注册/自动绑定都会复制该角色）。

> 固件 OTA 升级：在后台固件版本/发布任务里登记的必须是 **app-only bin**（例如
> `firmware/build/stack-chan.bin`），不是 `merged-binary.bin`；`fileUrl` 要能被设备直接
> GET（可用 `/api/stackChan/uploadFile` 上传后返回的公网 URL）。

## 3. 设备侧接口（固件使用，Go 兼容响应 `{"code":0,...}`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/device/ota`（+ `/activate`） | Java 原生 OTA；设备已绑定/自动建档后返回 websocket + 固件信息 |
| GET | `/api/stackChan/device/user` | 固件 Setup 页显示绑定用户名 |
| GET | `/api/stackChan/device/info` | 设备信息（mac/name/uid/bind_time）；固件只依赖 name |
| PUT | `/api/stackChan/device/info` | 修改当前设备名 |
| POST | `/api/stackChan/device/unbind` | 删除设备档案（下次 OTA 重新自动绑定） |
| POST/PUT | `/api/stackChan/device` | Go v1 设备创建/更新兼容（设备已建档时改名；POST 幂等返回 id=0） |
| GET | `/api/stackChan/device/randomList` | 在线设备随机列表（来自伴侣 WS 在线池，排除当前设备） |
| GET | `/api/stackChan/apps` | App Center 已上架应用列表 |
| GET/POST/PUT/DELETE | `/api/stackChan/dance`、`/api/stackChan/danceData` | 设备跳舞数据 CRUD |
| GET | `/api/stackChan/musicList` | Go v1 兼容静态音乐列表 |
| POST | `/api/stackChan/uploadFile` | 文件上传（设备 HMAC 或 App token） |
| GET | `/api/stackChan/file/**` | 公开只读下载（App Store 图标/固件包、音乐等） |

社区/社交（设备 HMAC，Go 兼容 `code=0`；App 当前无 UI）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/stackChan/post/add` | 发帖（content_text/content_image） |
| GET | `/api/stackChan/post/get?page&pageSize` | 帖子时间线（含设备名、评论） |
| DELETE | `/api/stackChan/post/delete` | 删除自己的帖子（级联删评论） |
| POST | `/api/stackChan/post/comment/create` | 发评论 |
| GET | `/api/stackChan/post/comment/get?postId&page&pageSize` | 评论分页 |
| DELETE | `/api/stackChan/post/comment/delete` | 删除自己的评论 |
| POST/GET | `/api/stackChan/pano` | 全景图上传/列表 |
| POST | `/api/stackChan/friend` | 添加好友（双向幂等，不能加自己） |

## 4. App 兼容接口（`/api/stackChan/v2/**`，Go 兼容响应）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/v2/user/login` | 用户名/密码登录，返回 `data.token` |
| POST | `/v2/user/registration` | 直接建号（无邮箱验证码），返回 NodeBB 风格字段 |
| GET | `/v2/user` | 当前用户资料（camelCase 字段） |
| GET | `/v2/devices` | 当前用户设备列表（mac=12 位小写纯 hex） |
| POST | `/v2/device/bind`、`/v2/device/activate_bind` | 绑定/激活绑定（已属当前用户幂等） |
| PUT | `/v2/device/update` | 修改设备名 |
| POST | `/v2/device/unbind` | 解绑（删除设备档案） |
| GET/POST/PUT/DELETE | `/v2/dance`、`/v2/danceData` | 跳舞 CRUD（danceData 原样 JSON 数组） |

## 5. 伴侣 WebSocket（`/api/stackChan/ws`，server:8091）

- 握手：`?deviceType=StackChan`（设备，HMAC）/ `?deviceType=App&deviceId=<App实例UUID>`（App，Sa-Token + 设备归属）。
- 帧格式：1 字节 msgType + 4 字节大端长度 + payload；文本帧原样转发。
- 覆盖消息：Opus、JPEG、ControlAvatar、ControlMotion、On/OffCamera、Text、通话请求/拒绝/接听/挂断、
  设备名读写、Dance、GetAvatarPosture、On/OffPhoneScreen、AimedTakePhoto、DeviceOnline/Offline、Ping/Pong、AppLanAddress。
- 心跳：服务端每 5 秒 ping(0x10)，客户端回 pong(0x11)；90 秒无消息清理连接。
- 部署注意：Nginx 必须给 `/api/stackChan/ws` 放行 WebSocket Upgrade（`web/nginx.conf` 已加）。

## 6. 数据库变更

- 新表：`sys_stackchan_dance`、`sys_stackchan_app`、`sys_stackchan_post`、`sys_stackchan_post_comment`、
  `sys_stackchan_pano`、`sys_stackchan_friend`（DDL 见 `db/init.sql`）。
- 已有库升级（按顺序执行，两个脚本均幂等）：
  ```bash
  mysql -u<user> -p <db> < db/upgrade/2026-11-04__stackchan_dance_app.sql
  mysql -u<user> -p <db> < db/upgrade/2026-11-04__stackchan_community.sql
  ```
- 不迁移 Go 版旧数据；用户/设备继续复用现有 `sys_user`/`sys_device`。

## 7. 已验证 / 未验证

已验证（本机 JDK21 + Maven 3.9.11，离线仓库）：

```bash
mvn -B -o -pl xiaozhi-server -am compile -DskipTests     # BUILD SUCCESS
mvn -B -o -pl xiaozhi-server -am test -Dtest='*ArchTest' # 58 tests, 0 failures
cd web && npx vue-tsc --build --force                    # 通过
cd web && npx eslint src/views/StackChanView.vue src/services/stackChan.ts  # 通过
```

未验证：真机 OTA/语音、App 构建与登录绑定、ESP-IDF 固件编译（本机无 ESP-IDF / Flutter）。

本机运行态冒烟（2026-11-04）已通过：

- 打包 `xiaozhi-server-6.1.0.jar`，用独立实例（18091）+ `XIAOZHI_DEVICE_AUTH_SECRET` + `HOST_IP=127.0.0.1` 验证；
- admin 登录、App Store 后台 CRUD、新增管理端跳舞/帖子/全景/好友接口全部通过；
- OTA POST 自动建档、设备 HMAC token（正确通过 / 伪造拒绝 / 换 MAC 拒绝）；
- 设备侧 `/device/user`、`/device/info`、`/device/randomList`、`/musicList`、跳舞、帖子/评论、全景、好友、文件上传/公开下载；
- App v2 登录/用户/设备列表/绑定/改名/解绑/跳舞 CRUD/agent restore；
- 伴侣 WebSocket（鉴权开启）：设备 HMAC + App Sa-Token 连接、0x17 在线通知、设备→App 文本、App→设备 ControlMotion。

**当前本机正在运行的 8091/8092 是旧配置旧代码**：OTA token 为空（设备鉴权未开启）、
dialogue 注册到 Redis 的地址是 `ws://2.0.0.1:8092`、新增管理端接口返回 404；需要按下面环境变量重启两个进程。

### 本地源码联调注意

1. **`.env` 只对 Docker Compose 生效**。`bin\all.ps1`/IDE 启动要在 PowerShell 或运行配置里显式设置
   `XIAOZHI_DEVICE_AUTH_SECRET`、`XIAOZHI_STACKCHAN_AUTO_PROVISION` 等环境变量。
2. **必须设置 `HOST_IP` 或 `XIAOZHI_SERVER_DOMAIN`**：不设时按网卡探测，Windows 多网卡/VPN 可能选到
   错误地址（实测得到 `2.0.0.1`，导致 OTA 的 WS 地址和文件上传 URL 都不可达）。局域网联调填本机
   局域网 IP，例如 `HOST_IP=192.168.1.100`；有域名则填 `XIAOZHI_SERVER_DOMAIN`。
3. **dialogue 进程（8092）必须启动**：OTA 下发的语音 WS 地址固定是 8092；只启动 server 时设备能连
   OTA 但无法对话。伴侣 WebSocket 在 server 的 8091 上，不受影响。
4. 不配 `XIAOZHI_DEVICE_AUTH_SECRET` 等于**不鉴权**（本地测试方便，对外部署必须配）；
   配了之后必须与固件 `CONFIG_STACKCHAN_DEVICE_AUTH_SECRET` 一致。
5. 管理端新页面需要重新构建/部署 `web`；菜单在 **设备中心 → StackChan**。
6. **改完后端 Java 代码必须重启 server 进程**，刷新浏览器不会让新增的 `/api/stackChan/admin/**` 生效。

## 8. 当前边界（验收前必读）

1. 管理端新增「设备中心 → StackChan」页面，共 6 个页签：
   - **设备**：列表（首次 OTA 后自动出现）、改名/切换角色、重启、解绑、跳转设备管理；
   - **跳舞**：全设备列表、新增/编辑（动作 JSON、背景音乐）、删除，与 App 的跳舞数据同源；
   - **社区**：帖子/评论列表、删除帖子（级联删评论）；
   - **全景**：全景图列表、新增、删除；
   - **好友**：好友关系列表、删除；
   - **App Store**：条目增删改查、上架/下架、图标/固件上传。
   管理端接口 `/api/stackChan/admin/**`（仅登录校验，不新增权限键）。
2. App 内的 AI Agent/MCP/Chat History 入口已隐藏：角色/模型/MCP 统一在 Java 管理后台配置
   （原来的 xiaozhi.me 直连已不可用）。
3. 官方 App 与自编译固件的 BLE 密钥不匹配；配网使用自建 App（`app/keys/ble_private_key.pem`），
   或先用官方固件+官方 App 配网再刷自定义固件（保留 NVS）。
4. 社区/全景/好友/随机在线设备/音乐列表等 Go 未启用模块已补齐为后端接口（见 §3），
   App 侧暂无对应页面；Go 的 `/stackChan/xiaozhi/*` 开发者接口按设计不再迁移。
5. 家庭自用模式：App 请使用 admin 账号登录（设备自动建档到 admin）；新注册的普通用户默认无法接管
   已归属 admin 的设备。
6. 伴侣 WebSocket 连接池为服务进程内状态；多实例部署需要额外 Redis 路由（当前家庭单实例够用）。
