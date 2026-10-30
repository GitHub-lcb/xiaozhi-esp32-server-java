# 变更日志

## [6.2.0] - 未发布

### 新功能

- feat: **OTA 分批升级**。新增固件版本库（`sys_firmware`）、升级任务（`sys_firmware_release`）与升级记录
  （`sys_firmware_upgrade_log`）。任务支持全量 / 按比例（按设备ID哈希分桶） / 白名单三种投放策略，
  可设时间窗；设备 `POST /api/device/ota` 时按「任务进行中 + 时间窗内 + 芯片型号匹配 + 目标版本高于当前版本 +
  命中策略」挑出最高版本下发，并记录一条升级记录。没有命中任何任务时行为与之前一致，老部署不受影响。
- feat: 后台新增「固件升级」页面（`/firmware`），三个 Tab：固件版本、升级任务、升级记录；权限键 `system:firmware`。

- feat: **设备接入一键自检**。新增 `GET /api/device/{deviceId}/self-check`，一次返回绑定、在线状态、设备信息完整度、
  OTA 地址、实时通道、设备鉴权、固件可升级版本七项结论，每项带判定依据与修复建议；设备页操作列新增「接入自检」。
- feat: **运维指标对接**。接入 Actuator + Micrometer Prometheus，暴露 `/actuator/health`、`/actuator/prometheus`；
  新增 `xiaozhi.ota.check`、`xiaozhi.ota.upgrade.delivered` 与 `xiaozhi.dialogue.servers` 指标。
  抓取请求带 `X-Ops-Token`（或 `Authorization: Bearer`）头即可免登录，令牌由 `XIAOZHI_OPS_TOKEN` 配置，
  只放行 health 与 prometheus 两个只读端点，其余 actuator 端点仍要求登录。

- feat: **MQTT 设备通道**。dialogue 作为 MQTT 客户端连接 broker，订阅设备主题后把上行消息交给与 WebSocket
  相同的 `MessageHandler`，对话/打断/TTS 不需要为 MQTT 再实现一遍；音频按协议用 base64 放进 JSON。
  新增 `sys_device_channel` 记录通道类型、在线状态与最近消息时间，巡检任务把超过 TTL 没消息的置为离线。
  OTA 响应在 `xiaozhi.mqtt.enabled=true` 时下发 `mqtt` 块（endpoint / client_id / publish_topic / 账号密码），
  设备可自行选择长连接通道。设备自检新增「MQTT 通道」一项。

- feat: **主动消息推送**。新增 `sys_push_message` 与 `/api/push` 一组接口、`/push` 后台页面：可按设备或按用户
  （展开其名下设备）选目标，发送时只投在线设备、离线跳过并计数。双进程下 server 拿不到 WebSocket 会话，
  经 Redis 广播 `xiaozhi:device-push` 交给持有会话的 dialogue 实例，由 `Persona.speakNow` 直接合成播报，不过 LLM；
  播报完成后设备回到 IDLE，会话不关闭。

- feat: **语音提醒与闹钟**。新增 `sys_reminder` 与 `/api/reminder` 接口、`/reminder` 后台页面：支持一次性、每天、
  工作日三种重复方式。server 每 30 秒扫描到点提醒，在线设备复用主动推送的 Redis 通道播报，离线跳过一次
  （重复任务顺延到下一次、一次性任务记为「已错过」）；服务器停机跨过多次时，下一次时间会跳过已经过去的时刻，
  不会补播一串旧提醒。

- feat: **设备远程控制**。新增 `sys_device_command` 与 `POST /api/device/{deviceId}/command`，设备页操作列新增
  「远程控制」（重启 / 设置音量 / 自定义工具调用）。动作翻译成设备 MCP 工具（默认 `self.reboot`、
  `self.audio_speaker.set_volume`，可用 `xiaozhi.device-command.*` 覆盖），经 Redis 广播交给持有会话的
  dialogue 执行真实调用并回填成功/失败与结果；设备离线时在下发前就拒绝，不会留下停在「已下发」的记录。

- feat: **设备截图**。新增 `sys_device_screenshot` 与 `POST /api/device/{deviceId}/screenshot`，设备页操作列新增
  「截图」并在弹窗里预览回传图片。截图复用远程控制通道调用设备截图工具（默认 `self.screen.snapshot`，
  可用 `xiaozhi.device-command.screenshot-tool` 覆盖）；回传的 http 地址或 data URI 由 dialogue 回填到记录，
  纯 base64 不落库（体积不可控），按失败处理并提示改用 URL 回传。

- feat: **知识库（RAG）后端**。新增 `sys_knowledge_base` / `sys_knowledge_doc` / `sys_knowledge_chunk` 与
  `/api/knowledge*` 接口：文档上传后经 Tika 解析（PDF/Office/HTML/纯文本）、按 500 字 + 50 重叠切片、
  用 EmbeddingModel 向量化入库，检索走余弦相似度取 topK，并提供 `/api/knowledge/search` 做召回测试。
  向量模型取知识库的 `embeddingConfigId`，未配置时回退 `xiaozhi.knowledge.embedding-config-id`。
- feat: **知识库接入对话与后台页面**。角色绑定的知识库会在每轮对话前检索 topK（默认 4 条、单条截断 400 字），
  作为系统消息插在角色提示词之后注入；检索失败或没有命中时按无知识库继续回答，不影响对话主链路。
  新增 `/knowledge` 后台页面：知识库增删改、文档上传（拖拽即索引）、文档状态与分片数、检索测试面板。

- feat: **声纹识别**。新增 `sys_voiceprint` 与 `/api/voiceprint`（列表 / 注册 / 删除 / 识别测试），后台
  `/voiceprint` 页面可上传单人录音注册家庭成员、再用一段录音测试能认出谁。向量提取走 sherpa-onnx
  `SpeakerEmbeddingExtractor`（模型路径 `xiaozhi.voiceprint.model-path`，默认 `models/speaker/speaker.onnx`），
  比对用余弦相似度、阈值 `xiaozhi.voiceprint.threshold`（默认 0.62）——宁可认不出也不认错人。
  模型不在时接口明确报「模型未就绪」，不会静默返回空向量。

- feat: **监控面板（活跃度 / 消息量）**。新增 `/api/stats/overview`、`/api/stats/messages/daily`、
  `/api/stats/devices/top`，Dashboard 增加概览卡片（设备总数、在线设备、今日消息、近 7 天消息）与 7 日消息柱状图。
  聚合查询走只读的 `StatsMapper`（COUNT / GROUP BY），趋势按天补齐 0、天数夹在 1-90，避免前端传大数拖死查询。

- feat: **长期记忆（人工维护）**。新增 `sys_memory_fact` 与 `/api/memory-fact`、`/memory-fact` 页面：可以按成员
  （或全家人）录入"应该记住的事"。每轮对话按「说话人 + 全家」两层取最近 8 条注入提示词，声纹认出了妈妈就只给
  妈妈的记忆，避免把别人的偏好套到当前说话人身上；注入文本带"与用户当前说法冲突时以用户为准"的约束。
  会话结束时可按开关自动提炼（`xiaozhi.memory.auto-extract-enabled`，默认关闭；用户发言少于 3 条不触发），
  管理端可用 `source` 区分人工与自动，也可手动触发「从最近对话提炼」。

- feat: **记忆图谱**。新增 `sys_memory_entity` / `sys_memory_relation` 与 `/api/memory-graph`、`/memory-graph` 页面。
  事实落库时用关系词表抽取「谁 关系 什么」三元组（`妈妈喜欢乌龙茶` → 妈妈/喜欢/乌龙茶），人工录入与自动抽取两条路径
  都会触发，抽不出来就跳过、不影响事实本身；关系以「妈妈 喜欢 乌龙茶」的形式随长期记忆一起注入对话（最多 10 条）。

- feat: **对话指标（Token / 时延）**。新增 `sys_dialogue_metric`，每轮对话落一条（prompt/completion/total tokens 与
  本轮耗时），`GET /api/stats/dialogue` 返回轮次、token 合计与平均时延；Dashboard 增加「对话轮次 / Token 消耗 /
  平均响应时延 / 活跃设备 Top3」四项，监控面板至此覆盖活跃度、消息量、Token 与时延四个维度。

- feat: **海量工具调度**。新增 `ToolSelector`：工具总数超过 `xiaozhi.tools.max-count`（默认 12）时，
  按本轮用户问题与工具名/描述的字符重合度粗排，只把最相关的一批交给模型；工具数不超过阈值时原样下发，
  不改变现有行为。纯本地计算，不依赖 embedding，也不额外消耗 token。

- feat: **曲库管理台（重构）**。后台「智能体 → 曲库」从「一个上传按钮 + 一张裸表」重做为可用的一等页面：
  统计卡（曲目总数 / 已上架 / 累计点播 / 占用空间）、关键字与状态筛选、服务端分页排序、编辑歌名/歌手/别名、
  上架/下架、删除、扫描目录同步（新文件自动登记、文件缺失自动下架）、在线试听（签名 URL + 原生音频控件）、
  点播记录分页（补设备名与命中歌名）。后端接口从「一次性全量列表」升级为
  `GET /api/file/music`（分页/关键字/状态/排序）、`/stats`、`/plays`、`PUT /{musicId}`、
  `DELETE /{musicId}`、`POST /sync`、`GET /{musicId}/audio-url`，新增权限键
  `system:file:api:update|sync|preview`；列表/播放/删除语义保持不变，设备点播链路不受影响。
  补齐 `music.*` 中英文文案 70 余条，页面不再出现裸翻译 key。

- fix: **曲库页打开就「权限不足」**。旧升级脚本只建了 `system:file:api:list/plays/delete` 三条权限行，
  没有授权给管理员角色（`authRoleId=1`），管理员也会被 Sa-Token 拦成 403。现在升级脚本按 permissionKey
  幂等补授权，后端曲库端点同时声明 `orRole="admin"` 兜底；架构测试新增「init.sql 里每条权限都必须授给管理员」
  的守卫，避免下次新增功能再漏。`db/upgrade/2026-10-30__music_console.sql` 可在存量库上一次补齐。

- fix: **后台上传的歌设备播不到**。`docker-compose.yml` 原来只把宿主机 `./music` 挂进 dialogue，
  server 容器里的 `/app/uploads/music` 还是独立的命名卷：后台上传写进命名卷、设备端读宿主机目录，两边互相看不见。
  现在 server 也挂同一份 `./music`，后台「上传 / 删除 / 同步」与设备点播操作同一批文件。

### 升级注意

- 已建好的库执行 `db/upgrade/2026-10-30__music_console.sql` 补曲库表、权限行与管理员授权（幂等，可重复执行），
  然后清一次 Redis 权限缓存（`XiaoZhi:Permission*`）或重启 server；也可在服务器上直接跑 `fix-music-permissions.sh`。
- `docker-compose.yml` 新增 `./music:/app/uploads/music` 挂载，升级后 `docker compose up -d` 让 server 重新挂载；
  宿主机 `./music` 里的 mp3 会同时被后台曲库与设备点播看到。
- 已建好的库执行 `db/upgrade/2026-10-28__firmware.sql` 补表与权限（含 `sys_knowledge_*`、`sys_device_screenshot`、`sys_device_command`、`sys_device_channel`、`sys_push_message`、`sys_reminder` 与对应权限）；新装环境直接执行 `db/init.sql`。
- 要用 MQTT 通道需配 `XIAOZHI_MQTT_*` 一组环境变量（`XIAOZHI_MQTT_ENABLED=true` 才启用），并让固件重新走一次 OTA 拿到 mqtt 配置。
- 要用 Prometheus 抓指标，需配置 `XIAOZHI_OPS_TOKEN`，并在抓取配置里带同名请求头。

---

## [6.1.0] - 2026-09-19

### 💥 升级注意

- **时间统一按应用所在时区**。库里的时间列不带时区，此前一部分由应用写入、一部分由数据库的 `NOW()` 与列默认值写入，
  数据库与应用不在同一时区时（数据库容器默认 UTC、云数据库）同一张表里会出现差整小时的两种时间。现在建立数据库连接时
  会把会话时区设成应用的时区，SQL 不再读数据库时钟。应用时区由 `TZ` 决定，compose 默认 `Asia/Shanghai`。
  启动日志出现「数据库时钟与应用时钟对不上」时，检查 `TZ` 与两台机器是否对时；自己配过 `connection-init-sql` 的不会被覆盖。
  存量数据不回填。
- **OTA 下发给设备的时区偏移不再写死东八区**，改为跟随应用时区并随夏令时变化。服务器不在东八区、
  又希望设备显示北京时间的，给 server 设 `TZ=Asia/Shanghai`。
- **`sys_user_auth` 列名改为驼峰**（`user_id` → `userId` 等，V88 启动时自动执行，数据保留）。直接对这张表写过 SQL 的要跟着改列名。

### 对话与音频链路
- feat: 播放中的插话交给模型判定是不是在对设备说话，不是则续播；模型输出不规整时按宽松 JSON 解析，不再静默失效
- feat: 语音识别支持角色级热词，腾讯、火山、FunASR 直传给服务商
- feat: 本地语音合成增加并发准入，超出处理能力时丢句而不是无限排队，可配并发数与推理线程数
- feat: 本地推理（识别、合成）的 CPU 核预算按正在使用的服务动态分配，只用其中一种时不再空着大半核数；闲置 10 分钟后让出份额
- fix: SenseVoice 解码并发按核预算与推理线程数推导，不再超发到整机核数
- fix: 唤醒词录音默认不落盘，避免不受保留期管理的文件持续堆积
- fix: VAD 静音计时、识别与传输超时改用单调时钟，系统对时不再影响收句时机与超时判断

### 架构与规约
- refactor: 取当前时间、Instant 与 LocalDateTime 互转统一走 `DateUtils`，测试可固定时钟；落库与接口模型用 `LocalDateTime`，
  运行时时刻用 `Instant`，`Date` 只留在第三方 SDK 与协议要求的位置
- refactor: `Conversation` 最小构造改静态工厂，`rawMessages` 返回快照，摘要输入带消息元数据前缀
- refactor: `Persona` 的 `Conversation` 约束为非空，清理各处判空
- refactor: 方法名按意图命名，不再枚举步骤
- refactor: `UserAuthDO` 继承 `BaseDO`，去掉逐字段的列名注解
- test: 新增架构守卫：取时入口、Mapper XML 不读数据库时钟、模型时间字段命名、DO 列名驼峰、镜像发布流水线
- test: 新增多设备并发生命周期压测；修复播放打断与识别流出错两处用例的竞态

### 修复
- fix: 监控页「近 N 分钟」类时间窗改由应用时钟计算，与消息写入时间同口径
- fix: 摘要接口的时间不再按写死的东八区渲染，与消息列表一致
- fix: 智能体发布时间此前按 UTC 渲染，显示比实际早 8 小时
- fix: 消息分页的起止时间改为 `LocalDateTime`，不再经过 JDBC 的时区换算
- fix: 角色页模型下拉的硬编码中文改用国际化文案
- update: 模型厂商与模型列表更新

### 部署
- fix: 镜像发布流水线只在本仓库运行；fork 后要自己发布镜像的，把 `docker.yml` 里的仓库名换成自己的

---

## [6.0.0] - 2026-09-18

### 💥 升级注意

- **Docker 数据卷会换名**。compose 现在固定项目名 `xiaozhi`，卷名从 `<所在目录名>_mysql_data`
  变成 `xiaozhi_mysql_data`。老部署直接 `docker compose up -d` 会挂到新的空卷上，数据看着像丢了
  （实际仍在旧卷里）。要继续用旧卷，升级前在 `.env` 里写 `COMPOSE_PROJECT_NAME=<原来的目录名>`。
- **compose 服务与文件改名**：服务 `node` → `web`，`Dockerfile-node` → `Dockerfile-web`。
  主 compose 默认拉 GHCR 镜像，从源码构建改为 `-f docker-compose.yml -f docker-compose.build.yml`。
- **`.env` 不再随仓库分发**，改为 `.env.example`；本地已有的 `.env` 不受影响。
- **前端不再写死后端地址**：WebSocket 与静态资源按页面地址推导，容器部署由 web 容器的 nginx 反代
  `/api`、`/ws`、`/audio`、`/uploads`。自建反向代理的部署要把 `/ws/` 也代理到 dialogue，
  或继续用 `VITE_WS_URL` 指定绝对地址。

### 架构与规约
- refactor: 读写两条路径分开——读侧 Service 直接出 BO/投影，Resp 组装收回 xiaozhi-server；写侧走 AppService → 聚合根 → Repository
- refactor: template 包降级，写路径收进 ServiceImpl（Mapper + MapStruct `updateDO`）
- refactor: 分页信封统一 `PageResult`，Req/DO 不再越过 Service 层
- refactor: 删除只做一行转发的 AppService、零调用的 Convert 方法与 `AuthUtils`
- refactor: `sys_code` 独立成 `verifycode` 包，单表读写统一走 MyBatis-Plus，XML 只留 JOIN/子查询
- test: 新增架构守卫（模块边界、Controller 出参、Mapper XML、组件扫描覆盖、敏感字段）与存量违规登记

### 对话与音频链路
- feat: WebSocket 二进制协议 v2/v3，打通服务端 AEC 的时间戳对齐
- feat: 接入 listen mode 与设备侧 AEC，打断改为 ASR 首字触发；误打断可续播
- feat: STT 流式识别中间结果回调；采集设备补发的唤醒词前置音频
- feat: 角色系统提示词改为模板渲染，补语音对话约束
- fix: 音频流写入串行化，收句不再被并发送帧挤掉
- fix: 打断后截断对话历史到用户实际听到的位置，并丢弃编码器残留样本
- fix: 消息窗口按对话组边界裁剪，不再留下孤儿工具消息
- fix: VAD/AEC 原生资源随会话关闭释放
- perf: 缓存 ChatModel 与 EmbeddingModel 实例

### AI 能力
- feat: 新增 Web 聊天（`WebChatController` + `WebChatService`），ai 模块弱化 device 概念改为 owner
- feat: 火山语音合成/识别升级 2.0，适配火山思考模型
- feat: 新增 sherpa-onnx 本地语音识别（SenseVoice），模型就位时作为本地识别的默认选项，Vosk 保留为备选
- feat: 新增 S3 对象存储
- fix: 工具调用递归超过五层后停止提供工具并要求模型收尾；同一轮多个工具调用只播一次提示
- fix: TTS 文本清洗去除 markdown 结构与括号舞台指示

### 安全
- feat: 设备接入鉴权与视觉接口签名，图片上传补安全校验
- fix: 审计日志凭证字段打码，明文口令与密钥不再落库
- fix: AI 运行时配置查询必须带用户，避免退化成全库查询
- fix: 用户邮箱与手机号补唯一索引

### 部署与文档
- feat: `docker compose up -d` 一键启动，默认拉 GHCR 预构建镜像；源码构建移到 `docker-compose.build.yml`
- feat: 镜像发布流水线 `.github/workflows/docker.yml`，打 tag 即发布 server/dialogue/web 三个镜像
- feat: `bin/*.sh` 与 `bin/*.ps1` 启动前自检 JDK 版本、模型与原生库、MySQL/Redis 连通性；ps1 补齐 profile 参数
- feat: 前端产物不再写死后端地址，WebSocket 与静态资源按页面地址推导，web 容器 nginx 反代 `/api`、`/ws`、`/audio`、`/uploads`
- fix: compose 补健康检查与固定项目名，dialogue 等 server 建完表再启动
- fix: 前端镜像构建改为串行产出产物，不再在 vue-tsc 与 vite 并行时 OOM
- fix: `download_stt.sh` 支持 `none`，`VOSK_MODEL_SIZE=none` 不再让镜像构建中断
- docs: 新增 `docs/CONFIGURATION.md`（首次配置、环境变量、安全默认值）与 `docs/FAQ.md`（常见问题统一收口）
- docs: README 与三份部署文档按一键部署重写，Docker 部署不再需要 clone 仓库

---

## [5.0.0] - 2026-04-11

### 💥 重大变更
- **refactor!: 项目拆分为多模块架构** 
  - 从单体项目重构为 Maven 多模块：`xiaozhi-common`、`xiaozhi-service`、`xiaozhi-ai`、`xiaozhi-dialogue`、`xiaozhi-server`
  - 模块间通过窄接口解耦（AI 模块仍依赖 xiaozhi-service，包括两个 Mapper 直连）
  - Web 组件从 common 迁移至 server 模块，职责更清晰

### 新增功能

#### 架构 & 基础设施
- feat: 引入 Flyway 数据库版本管理
- feat: 引入 DDD 领域驱动设计模式（聚合根、领域事件、Pipeline）
- feat: 新增通用 Shell 脚本用于服务管理
- feat: 添加前端单元测试 (Vitest) 和 E2E 测试 (Playwright) 基础设施
- feat: 新增审计日志注解 (AuditLog)，增强操作追踪

#### 权限 & 安全
- feat: 重构权限管理系统
- feat: 新增权限管理页面（前端）
- feat(auth): 暴露角色权限并在用户管理中展示

#### AI & 对话
- feat: 增强长期记忆会话，支持图检索能力 (Graph Retrieval)
- feat: 重构知识库，整合长期记忆与声纹识别
- feat: 知识库 Pipeline 重构
- feat: 实现 Edge TTS 流式语音合成
- feat: 添加从输入流读取 Ogg Opus 格式音频帧的功能

#### 前端
- feat: 新增音频播放器 fallback 支持

### 重构 & 优化

#### 异常处理统一化
- refactor: 全面统一 Controller 层异常处理（共 4 批次）
- refactor: 统一 CRUD 操作失败语义（create/update/delete）
- refactor: 统一语音克隆、声纹识别、用户校验等模块异常处理
- refactor: 强化 OTA 协议、上传/TTS、特殊端点的错误处理
- refactor: 消除 null 和 zero 失败语义，内部 BO 创建不再返回 null

#### 领域模型 & 命名
- refactor: SysSummary 替换为 SummaryBO
- refactor: ConversationTurn 重命名为 DialogueTurn
- refactor: functionNames 重命名为 mcpList
- refactor: 统一 auth role permission 命名
- refactor: 用户角色字段统一为 authRole
- refactor: 重构 ApiResponse 统一响应格式
- refactor: 重构 token 管理及 message 发送方法

#### API & 接口
- refactor: 重构 API 端点为 RESTful 风格
- refactor: 分页参数从 start/limit 统一为 pageNo/pageSize
- refactor: MCP Controller 错误契约标准化
- refactor: 拆分 mcpServer 与 mcpEndpoint 模块
- refactor: 替换手动 login id 转换为 sa-token typed API

#### 架构解耦
- refactor: 适度解耦 Session 架构
- refactor: Dialogue 和 Server 模块只扫描需要的包
- refactor: 移除 req 对象在 dialogue 和内部运行时流中的泄漏
- refactor: 用户 ID 在 service 和 task 边界显式传递
- refactor: 运行时路径配置整合，减少 STT/TTS 服务中的硬编码路径
- refactor: 去除路径硬编码
- refactor: 统一缓存位置
- refactor: 统一领域事件
- refactor: 优化对话逻辑并去掉冗余的虚拟线程方法

#### 清理
- refactor: 移除未使用的 IntentDetector 类（同批提到的 ExitKeywordDetector 后来又加了回来，仍被 IntentService 使用）
- refactor: 移除未使用的 HttpSessionProvider、ResponseUtils、SessionProvider、DramaJson 类
- refactor: 移除声纹阈值自定义功能
- refactor: 移除多个过时的架构文档
- delete: 去掉知识库中关于 userId 多余的传递
- delete: 去除未使用的方法

### 修复
- fix: 修复若干 bug 及优化集群部署
- fix: 修复 endpoint 集群部署问题及 mcpServer 迁移依赖
- fix: 修复 CosyVoice 音频句子发送 bug
- fix: 修复角色更新后设备未及时更新问题
- fix: 修复 MQTT 重复构建对话消息与唤醒词多次发送 start 问题
- fix: 修复测试音效错误及音频缓存命中错误问题
- fix: 修复类启动错误问题
- fix: 修复调用错误方法

### Docker & 部署
- refactor: 统一 Docker 网络、添加资源限制、调整上传大小为 100MB
- update: Dockerfile-server 适配多模块构建
- update: Dockerfile-node 构建上下文调整为 web 目录
- update: .dockerignore 更新排除规则
- update: docker-compose.yml 移除无用的 maven_repo 卷

### 测试
- test: 完成 Controller 测试基线
---

## [3.0.0] - 2025-11-01

### 💥 重大变更
- **feat: 前端架构全面升级到 Vue3** 🎉
  - 完整迁移到 Vue 3.5.22 + Composition API
  - 使用 Vite 7 作为构建工具，提升开发体验和构建速度
  - 采用 TypeScript 5.9 增强类型安全
  - 状态管理升级到 Pinia 3
  - 路由升级到 Vue Router 4
  - 采用 Composables 模式重构代码，提高可复用性

- **feat: 后端架构全面升级与重构** 🚀
  - 引入 JWT 认证机制，增强安全性
  - 新增统一结果封装 (ResultMessage/ResultStatus)
  - 新增事件驱动架构 (ChatSessionOpenEvent、ChatAbortEvent 等)
  - 新增完整的权限管理系统 (RBAC)
  - Controller 层全面重构，代码结构更清晰

### 新增功能

#### 前端
- feat: 升级 Node.js 运行时到 v22
- feat: 引入现代化开发工具链
  - 使用 oxlint 和 ESLint 9 进行代码检查
  - 集成 Vue DevTools 8 用于调试
  - 采用 Prettier 3.6 统一代码风格
- feat: UI 组件库升级到 Ant Design Vue 4.2.6
- feat: 新增 @vueuse/core 工具库，提供丰富的组合式 API
- feat: 新增全局加载组件和错误边界
- feat: 新增浮动聊天组件，优化交互体验

#### 后端核心功能
- feat: 新增 JWT 认证系统 (JwtUtil)
  - 支持 Token 生成和刷新
  - 支持微信登录 Token
  - 支持自定义 claims
- feat: 新增微信登录服务 (WxLoginService)
- feat: 新增权限管理系统
  - 角色权限映射 (SysAuthRole, SysPermission, SysRolePermission)
  - 完整的 RBAC 权限控制
- feat: 新增验证码工具 (CaptchaUtils)
- feat: 新增邮件工具 (EmailUtils)
- feat: 新增短信服务 (SmsUtils)
- feat: 新增文件哈希工具 (FileHashUtil)
- feat: 新增音频增强工具 (AudioEnhancer)

#### AI & LLM
- feat: 新增 OpenAI LLM 服务 (OpenAiLlmService)
  - 支持流式响应
  - 支持深度思考模式
  - 支持 Function Calling
  - 新增 Token 回调机制
- feat: 新增 MCP (Model Context Protocol) 支持
  - MCP Session 管理
  - MCP 设备服务集成
- feat: 增强对话服务 (DialogueService)
  - 优化会话管理
  - 改进消息处理流程
  - 支持事件驱动
- feat: VAD 服务重大重构
  - 优化语音活动检测
  - 改进 Silero VAD 模型
  - 新增高级参数配置

#### 依赖更新
- update: 阿里云 SDK 全面升级
  - nls-sdk-transcriber: 2.2.1 → 2.2.18
  - nls-sdk-tts: 2.2.17 → 2.2.18
  - dashscope-sdk-java: 2.20.2 → 2.20.6
  - 新增阿里云短信服务 SDK 2.0.24
- update: Spring Boot 依赖更新
  - 新增 spring-boot-starter-data-redis (缓存增强)
  - spring-ai-starter-mcp-client 集成
- update: commons-io: 2.11.0 → 2.18.0
- update: okhttp: 5.0.0-alpha.14 → 4.9.3 (提升稳定性)
- update: 新增 okio 3.13.0

### 优化与改进

#### 前端优化
- perf: Vite 开发服务器性能大幅提升
- perf: 生产构建体积优化和加载速度提升
- perf: 优化路由守卫和权限检查
- update: Docker 镜像更新到 node:22-alpine
- update: 依赖包全面更新到最新稳定版本
- update: 优化开发环境配置和热更新机制
- dx: 更好的 TypeScript 类型推导和提示
- dx: 更快的热模块替换 (HMR)

#### 后端优化
- refactor: 全局异常处理增强 (GlobalExceptionHandler)
  - 新增资源未找到异常 (ResourceNotFoundException)
  - 新增未授权异常 (UnauthorizedException)
  - 统一异常响应格式
- refactor: 认证拦截器重构 (AuthenticationInterceptor)
  - 支持 JWT 认证
  - 优化权限验证逻辑
- refactor: 会话管理重构 (SessionManager)
  - 改进会话生命周期管理
  - 优化并发处理
- refactor: 消息处理器重构 (MessageHandler)
  - 优化消息流转
  - 改进错误处理
- refactor: WebSocket 处理器优化 (WebSocketHandler)
  - 增强连接管理
  - 改进异常处理
- refactor: 对话记忆系统优化
  - DatabaseChatMemory 重构
  - MessageWindowConversation 改进
  - Conversation 接口优化
- refactor: LLM 工具调用优化
  - ToolsGlobalRegistry 改进
  - XiaoZhiToolCallingManager 重构
  - 新增 NewChatFunction
- refactor: STT 服务优化
  - 所有 STT 提供商代码优化
  - 改进错误处理和日志
- refactor: 实体类优化
  - SysConfig, SysDevice, SysMessage, SysUser 改进
- refactor: Mapper XML 优化
  - 所有 Mapper 文件重构
  - SQL 优化
- refactor: Service 层全面重构
  - 新增事务配置 (TransactionConfig)
  - 优化业务逻辑
  - 改进数据访问层

### Docker 更新
- update: docker-compose.yml 配置优化
  - 改进服务依赖关系
  - 优化健康检查
  - 增强网络配置
- update: Dockerfile-node 升级到 Node 22

---

## [2.8.17] - 2025-07-16
### 新增
- feat: 新增 Swagger
- update: 模型增加辨识度标签
- update: 删除全局聊天多余缩小按钮
- update: 优化展示样式，可以切换浏览器标签页样式
- update: 实体采用 Lombok 方法
### 修复
- fix: 修复地址错误问题
- fix: 修复添加设备时验证码未生效问题
- fix: 修复 init SQL 脚本初始化缺少字段问题
- fix: 修复 issues #119 #120
### 样式优化
- style: 更新全局聊天缩放动画，更接近苹果效果
- 优化: 聊天样式
### 删除
- delete: 删除无用 log
### 重构
- refactor(stt): 优化 VoskSttService 类的代码结构
- refactor: 去掉多余 log

## [2.8.16] - 2025-07-02
### 其他变更
- refactor:vad重构，去除agc
- refactor:重构音频发送逻辑，按照实际帧位置发送

## [2.8.15] - 2025-07-01

### 修复
- fix:修复tag更新错误问题
- fix:修复设备在聆听时，修改角色配置导致缓存更新时多次查询数据库的问题
- fix:修复init初始化确实头像字段

### 其他变更
- refactor:优化token缓存，减少冗余代码
- update:阿里巴巴sdk日志级别改为warn

## [2.8.0] - 2025-06-15

### 新功能
- feat:增加logback输入 close #37
- feat:新增橘色设备量展示

### 修复
- fix(stt.aliyun): do not reuse recognizer
- fix(stt.aliyun): support long speech recognition
- fix: memory leak. Should clean up dialogue info after session closed

### 其他变更
- chore: update version to 2.8.0 [skip ci]
- update:角色返回增加modelName
- docs: update changelog for v2.7.68 [skip ci]
- chore: update version to 2.7.68 [skip ci]
- docs: update changelog for v2.7.67 [skip ci]
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]
- refactor(stt): simplify SttServiceFactory

## [2.7.68] - 2025-06-14

### 修复
- fix(stt.aliyun): do not reuse recognizer
- fix(stt.aliyun): support long speech recognition
- fix: memory leak. Should clean up dialogue info after session closed

### 其他变更
- chore: update version to 2.7.68 [skip ci]
- docs: update changelog for v2.7.67 [skip ci]
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]
- refactor(stt): simplify SttServiceFactory

## [2.7.67] - 2025-06-14

### 修复
- fix: memory leak. Should clean up dialogue info after session closed

### 其他变更
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]

## [2.7.64] - 2025-06-12

### 修复
- Merge pull request #98 from vritser/main
- fix(audio): merge audio files

### 其他变更
- chore: update version to 2.7.64 [skip ci]
- docs: update changelog for v2.7.63 [skip ci]
- chore: update version to 2.7.63 [skip ci]

## [2.7.60] - 2025-06-11

### 新功能
- Merge pull request #96 from vritser/main
- feat(tts): support minimax t2a

### 修复
- fix:修复阿里语音合成多余参数，删除
- fix(tts): tts service factory

### 其他变更
- chore: update version to 2.7.60 [skip ci]
- docs: update changelog for v2.7.59 [skip ci]
- chore: update version to 2.7.59 [skip ci]
- refactor(tts): add default implements
- docs: update changelog for v2.7.58 [skip ci]
- chore: update version to 2.7.58 [skip ci]

## [2.7.59] - 2025-06-11

### 新功能
- Merge pull request #96 from vritser/main
- feat(tts): support minimax t2a

### 修复
- fix(tts): tts service factory

### 其他变更
- chore: update version to 2.7.59 [skip ci]
- refactor(tts): add default implements
- docs: update changelog for v2.7.58 [skip ci]
- chore: update version to 2.7.58 [skip ci]

