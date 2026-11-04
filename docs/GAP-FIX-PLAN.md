# 缺口修复与细化方案

> 配套文档：[FUNCTIONAL-MODULES.md](./FUNCTIONAL-MODULES.md)（功能模块梳理，§9 有缺口总表）。
> 本文把「还没修完的缺口」细化到**可排期、可验收**的程度：现状证据 → 目标 → 改动清单（文件级）→ 接口/数据变更 → 兼容与回滚 → 验收方式 → 风险与工作量。
>
> 口径：基于当前工作区静态代码（2026-11-03）。已完成项在 §0 单列；本文其余章节只讲**未完成**的部分。
> 涉及外部服务商（TTS/STT/音色克隆）与第三方实例（Home Assistant、向量库）的方案，都标注了「需要凭据/实例才能联调」。

---

## 0. 本轮已修复（2026-11-03）

| # | 缺口 | 修复内容 | 验证 |
|---|---|---|---|
| 1 | 操作日志「只写不查」 | 新增 `GET /api/operation-log`（分页、模块/操作/用户/结果/时间范围筛选、单页上限 100、用户名回填）+ `/operation-log` 前端页面 + 权限键 `system:operation-log:api:list`；`db/init.sql` 与 `db/upgrade/2026-11-03__operation_log_query.sql` 幂等补权限 | `OperationLogControllerTest`（4）、`OperationLogServiceImplTest`（2）、`PermissionKeyContractArchTest` |
| 2 | 多实例 server 重复播报提醒 | service 模块新增 `ReminderScanTask`（`@Scheduled` + Redisson 锁 `lock:task:reminder-scan`），server 的 `ReminderAppService` 实现 `ReminderScanExecutor` 承接事务；锁覆盖到事务提交之后（分层理由：server 不允许直连 Redis，见 `ForbiddenDependencyArchTest`） | `ReminderScanTaskTest`（4）、`ReminderAppServiceTest`（3，保持原口径）、`ForbiddenDependencyArchTest` |
| 3 | compose 未下发 `JWT_SECRET_KEY` / 邮件 / 数据目录 / 域名 / 可信代理 / MQTT 细节 | `docker-compose.yml` 全部透传；`.env.example`、`docs/CONFIGURATION.md`、`docs/DEPLOY.md` 同步；新增 `TrustedProxyEnvBindingTest` 钉住可信代理环境变量名 | `TrustedProxyEnvBindingTest`（2） |
| 4 | dialogue JWT 密钥硬编码 | 改为 `${JWT_SECRET_KEY:...}`，与 server 同源注入 | 编译 + 配置类测试 |
| 5 | prod profile 内置默认域名 `ai.lichenbo.cn` | 移除隐式默认，留空按探测 IP 走 http/ws；要域名显式配 `XIAOZHI_SERVER_DOMAIN` | 配置评审；`ServerAddressProvider` 空域名分支已存在 |
| 6 | 旧文档数字/状态过时 | 验收清单表数/权限数、README Qdrant、路线图已落地项订正 | 见各文件 diff |

> 回归基线（本轮改动后必须保持全绿）：
> ```bash
> mvn -B -o -pl xiaozhi-server -am test -Dtest='*ArchTest' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
> cd web && npx vue-tsc --build --force
> ```
>
> 本轮实测（2026-11-03）：后端全量 `mvn -B -o test -Dtest=!ImagePublishGuardTest` → **1463 条通过**
> （`InitSqlGuardTest` 2 条因本机未配 MySQL 跳过；`ImagePublishGuardTest` 为本 fork 已知外部失败，显式排除）；
> 12 个 ArchTest 全绿；前端 `npm run type-check` 0 error、`npm run test:run` **70 个文件 / 558 条用例全绿**。

---

## 1. 真流式 TTS（音频级流式）

### 1.1 现状（实测，注意与 `STREAMING-DESIGN.md` 的旧描述有出入）

- 接口与过渡层**已经存在**：`StreamingTtsService.synthesizeStream(text)`、`FileBackedStreamingAdapter`、`TtsServiceFactory` 都已落地（`xiaozhi-ai/.../tts/`）。
- **真正 native 流式的只有 1 家**：`VolcengineTtsService`（`nativeStreaming()=true`）。`synthesizer` 链路已按 Flux 消费，其余 7 家由适配器「整句落文件再分块」，首包不会更快。
- 因此 `STREAMING-DESIGN.md` 的 P1 **已完成**、P2 只完成火山；文档需要按此更新（该文档本轮未改，避免与代码再次漂移，建议排期一并订正）。

### 1.2 目标

| 指标 | 现状 | 目标 |
|---|---|---|
| 长句首帧（用户说完 → 设备出声首帧） | 等整句合成（1.5–3s） | 云端 provider < 300ms 出首帧；整体 < 800ms |
| 打断响应（插话 → 停播） | < 200ms | 保持不退化 |
| 行为一致性 | — | 非流式 provider 与改造前完全一致（帧数/时长/顺序） |

### 1.3 改动清单（按依赖顺序）

| 步骤 | 文件 / 位置 | 改动 |
|---|---|---|
| 1 | `xiaozhi-ai/.../tts/providers/AliyunTtsService.java` | 实现 `StreamingTtsService`：CosyVoice/sambert 的流式接口逐块返回 16k/单声道/16bit PCM；`nativeStreaming()=true` |
| 2 | 同目录 `TencentTtsService` / `XfyunTtsService` / `MiniMaxTtsService` | 视厂商协议逐个实现；不支持的保持适配器包装（`nativeStreaming()=false`），不要硬凑 |
| 3 | `xiaozhi-ai/.../tts/SentenceHelper` + `FileSynthesizer` | 已按 Flux 消费，确认 chunk 边界：不足一帧的残字节要缓存到下一块（`OpusProcessor` 已有 `flushLeftover`，补流式场景的单测） |
| 4 | `xiaozhi-dialogue/.../playback/OpusRecorder` | 流式分块写入时保证 OGG 头只写一次；回归 `ScheduledPlayerSentenceBindingTest` |
| 5 | `sys_dialogue_metric` | 已采集 `ttsFirstChunkMs`；P4 增加 provider 维度标签（`sys_dialogue_metric` 加列需走升级脚本） |
| 6 | 测试 | 为每家 native provider 加「chunk 顺序/采样率/首包时序」单测；`FileBackedStreamingAdapter` 加「空音频/单帧/残帧」用例 |

### 1.4 接口与数据变更

- 不新增对外 HTTP 接口；`StreamingTtsService` 是内部能力接口。
- 可选：`sys_dialogue_metric` 增 `ttsProvider` 列（varchar(30)），用于按 provider 统计首包时延。**若要加列**：同时改 `db/init.sql` 与 `db/upgrade/<日期>__tts_provider_metric.sql`（幂等 `ADD COLUMN IF NOT EXISTS` 语义，MySQL 8 用存储过程或 `ALTER TABLE ... ADD COLUMN` + 存在性判断）。

### 1.5 兼容与回滚

- 风险控制：`nativeStreaming()` 默认 true，未实现者显式 false 走适配器；行为不变。
- 回滚：把对应 provider 的 `nativeStreaming()` 改回 false（或移除 `implements StreamingTtsService`）即回到旧路径；不改表、不改协议。
- **播放器 60ms 节拍与打断逻辑一行都不能动**（AGENTS.md 硬约束）；只换「音频来源」。

### 1.6 验收

1. 离线：用仓库内对话回归用例（协议 harness + `ScheduledPlayer*` 测试）验证帧数/时长/顺序与改造前一致。
2. 指标：`sys_dialogue_metric.ttsFirstChunkMs` 的 P50/P95 下降（Dashboard 可看平均时延）。
3. 真机：ESP32 实测「一句话问完到出声」；打断不退化。

### 1.7 风险与工作量

| 风险 | 缓解 |
|---|---|
| 厂商流式协议差异大、凭据联调成本 | 按厂商分批，每批单独开关；无凭据时只用适配器路径 |
| 残帧/采样率导致杂音 | 统一 16k/单声道/16bit，加断言；`OpusProcessor` 单测覆盖残帧 |
| 打断时上游回调仍在写 | 复用现有 `epoch` 代次校验，回调里丢弃过期代次 |

预估：阿里 1–2 天；腾讯/讯飞 各 1 天；测试与联调 2 天（不含厂商审核/开通）。

---

## 2. STT 双向流式（边说边出字）

### 2.1 现状

- `SttService.stream(flux, onPartialText)` 已支持中间结果（腾讯/阿里/火山/FunASR/讯飞云端）；本地 SenseVoice/Vosk 是离线整段。
- `DialogueService.onSttPartialText` 目前只用中间结果做「首字暂停播放」（barge-in 预判），**没有把 partial 下发给设备**；设备只能在收句后收到最终 `stt` 文本。

### 2.2 目标

- 设备端能看到「边说边出字」：识别中增量下发 `stt` 消息（带 `is_final=false` 语义，消息体保持现有 `stt` schema，避免固件改协议）。
- 首字更早触发打断判定（已有），并能在 partial 阶段就做回声/附和初筛（降低误打断）。

### 2.3 改动清单

| 步骤 | 文件 | 改动 |
|---|---|---|
| 1 | `xiaozhi-dialogue/.../dialogue/DialogueService.onSttPartialText` | 节流（如 ≥200ms 或文本增长 ≥2 字）后调用 `session.sendStt(...)`，带 `final=false`；最终结果仍走现有路径 |
| 2 | `xiaozhi-dialogue/.../communication/message/MessageSender` | `sendStt` 增加重载（文本 + final 标记），保持旧方法默认 final=true，固件按未知字段忽略 |
| 3 | `xiaozhi-dialogue/.../communication/domain/SttMessage`（或现有消息模型） | 增加可选字段 `final`（默认 true），兼容旧固件 |
| 4 | 本地 STT（`SenseVoiceSttService`/`VoskSttService`） | 明确「无 partial」：只在整段识别后发 final，避免上层空转 |
| 5 | 测试 | `MessageHandlerListenTest` 增加 partial 节流用例；协议 harness 增加「partial 中途 abort」用例 |

### 2.4 兼容与回滚

- 协议向后兼容：旧固件忽略 `final` 字段 → 行为不变。
- 回滚：`onSttPartialText` 只保留暂停逻辑即可（一个配置开关 `xiaozhi.stt.partial-to-device-enabled` 建议默认 false，灰度开启）。

### 2.5 验收

- 真机看到中间文本；打断判定不误触发；云端 provider 断流时 partial 停止但 final 正常/降级重放。

预估：2 天（含真机回归）。

---

## 3. 音色克隆接入真实服务商

### 3.1 现状

- 表（`sys_voice_clone`）、状态机（0/1/2）、页面（`/voice-clone`）、测试、`VoiceCloneClient` 接口全部就绪。
- 唯一实现是 `UnavailableVoiceCloneClient`：`provider()="none"`、`available()=false`、`enroll()` 直接抛「未接入音色克隆服务商」。

### 3.2 目标

- 阿里云 CosyVoice 声音复刻 / 火山声音复刻任一或全部接入；同步返回或异步任务轮询完成后更新 `sys_voice_clone.voiceId` 与 `state`。
- 克隆出的音色 ID 能被 `AliyunTtsService` 的 `resolveModel` 正确识别（代码注释已预留 `*-voiceclone-xxx` 格式）。

### 3.3 改动清单

| 步骤 | 文件 | 改动 |
|---|---|---|
| 1 | `xiaozhi-ai/.../tts/clone/` 新增 `AliyunVoiceCloneClient`（或 `VolcengineVoiceCloneClient`） | 实现 `VoiceCloneClient`：上传样本（本地/对象存储 URL）→ 调厂商上传/训练接口 → 返回 voiceId；异步任务按厂商接口轮询，结果写回由上层负责 |
| 2 | `VoiceCloneClient` 增加任务状态查询能力或回调参数 | 若厂商异步：接口改为「提交返回 taskId + 查询 taskId」，避免阻塞请求线程 |
| 3 | `xiaozhi-service/.../voiceclone/service/impl/VoiceCloneServiceImpl` | 调用真实 client；异步结果回写 `state/voiceId/errorMsg`；失败原因落库 |
| 4 | `sys_config`（configType=llm/tts） | 复用现有 TTS/LLM 配置里的 apiKey/ak/sk，不新增表 |
| 5 | 测试 | client 接口 mock 测「成功/厂商报错/超时」三条路径；`VoiceCloneServiceImplTest` 扩展 |
| 6 | 前端 `/voice-clone` | 状态轮询（`state=0` 时定时刷新），失败原因展示（页面已有字段） |

### 3.4 兼容与回滚

- 保留 `UnavailableVoiceCloneClient` 作为「未配置凭据」时的默认实现（`provider="none"`），接口明确报错；
- 回滚：把 `VoiceCloneClient` 实现切回 Unavailable 即可，表与页面不受影响。

### 3.5 验收

- 后台创建克隆 → 状态从处理中变就绪 → 角色选择该音色 → 设备播报使用新音色；失败路径有明确原因。
- **需要厂商账号、Key、实名/审核开通**，无凭据只能做 mock 联调。

预估：接口对接 2–3 天 + 联调 1–2 天。

---

## 4. 插件持久化与热加载

### 4.1 现状

- `PluginExtension`（SPI）+ `PluginRegistry`：只做 classpath 发现与进程内启停，重启回到全启用；插件承载自身 Spring Bean，无依赖隔离。

### 4.2 目标（分两级，不一次做大）

- **L1 持久化**：启停状态落库（`sys_plugin_state`：pluginId/name/version/enabled/updateTime），重启后按库恢复。
- **L2 热插拔（可选）**：上传 jar → 独立 ClassLoader 加载 → 注册/卸载工具；限制：只允许「无状态工具类插件」，禁止跨插件类依赖。

### 4.3 改动清单（L1）

| 步骤 | 文件 | 改动 |
|---|---|---|
| 1 | `db/init.sql` + `db/upgrade/<日期>__plugin_state.sql` | 新建 `sys_plugin_state` 表（幂等 `CREATE TABLE IF NOT EXISTS`） |
| 2 | `xiaozhi-common`/`xiaozhi-service` 新增 plugin 状态服务 | DAO + 启动时读取 → `PluginRegistry` 初始化时覆盖默认值 |
| 3 | `PluginController.toggle` | 先写库再改内存；失败回滚 |
| 4 | 测试 | `PluginRegistryTest` 扩展：状态恢复、未知插件、重复 toggle |

L2 需要自定义 ClassLoader/生命周期、工具注销机制，属于独立项目，建议单独立项。

预估：L1 1 天；L2 3–5 天。

---

## 5. 外部 MCP Server 接入（MCP Client 侧）

### 5.1 现状

- 依赖 `spring-ai-starter-mcp-client` 与 `CustomMcpSyncClientCustomizer` 在位，但**全仓没有外部 MCP Server 地址配置**，实际未启用。
- 设备端 MCP 是自定义协议（已完整），服务端 MCP Server（SSE）已完整。

### 5.2 目标

- 支持在后台配置外部 MCP Server（stdio/SSE 两种 transport），把其工具注册进对话工具表；与现有「工具排除」(`sys_mcp_tool_exclude`) 统一过滤。

### 5.3 改动清单

| 步骤 | 文件 | 改动 |
|---|---|---|
| 1 | `sys_mcp_server` 新表（name/transport/url/apiKey/enabled/超时等） + 升级脚本 | 幂等建表 |
| 2 | `xiaozhi-ai/.../mcp/client/` 新增 `McpServerRegistry` | 按库配置创建 `McpSyncClient`（Spring AI），启动/配置变更时重建；连接失败降级为「该 server 工具不可用」 |
| 3 | `ToolRegistrationService` / `SystemToolRegistrar` | 合并外部 server 的工具（命名加前缀避免与内置/设备工具冲突） |
| 4 | `McpToolExcludeService` | 排除维度增加 `mcpServer`（表已有该列语义，确认后复用） |
| 5 | 前端配置页 | 「插件」或「配置管理」中增加 MCP Server 配置子页（复用 `ConfigManager` 模式） |
| 6 | 测试 | mock MCP client 连接失败/工具列表为空/工具重名三种场景 |

### 5.4 安全与回滚

- 外部 MCP Server 属于「出网调用」：需超时、重试上限、并发上限（复用 `LocalInferenceBudget` 思路）；
- 写入类工具默认不注册（或走 `XIAOZHI_MCP_ALLOW_WRITE` 开关）；
- 回滚：`sys_mcp_server.enabled=0` 即不加载。

预估：3 天（不含第三方 server 联调）。

---

## 6. 知识库检索换向量库（十万级文档）

### 6.1 现状

- `sys_knowledge_chunk.embedding` 存 longtext JSON；`KnowledgeRetriever` 进程内全量余弦（类注释明确「到十万级再换向量库」）。
- README 里「Qdrant」是过时表述，代码/部署均无 Qdrant。

### 6.2 目标

- 抽象 `VectorStore` 接口，默认实现保持「MySQL 全量余弦」（小规模零依赖）；可选实现 Qdrant/pgvector（大规模）。
- 切换按知识库维度配置（`sys_knowledge_base` 加 `vector_store` 列），允许逐步迁移。

### 6.3 改动清单

| 步骤 | 文件 | 改动 |
|---|---|---|
| 1 | `xiaozhi-ai/.../knowledge/VectorStore` 新接口（upsert/search/delete） | MySQL 实现复用现有 `EmbeddingMath`；Qdrant 实现走官方 SDK（需新增依赖，放 `xiaozhi-ai`） |
| 2 | `KnowledgeIndexer` / `KnowledgeRetriever` | 改为依赖接口；删除文档时分片与向量同时删 |
| 3 | `sys_knowledge_base.vector_store`（varchar）+ 升级脚本 | 默认 `mysql`；切到 qdrant 时做一次重建索引任务 |
| 4 | 重建任务 | 后台按钮/接口（异步），进度写日志；失败可重试 |
| 5 | 测试 | 接口契约测试；MySQL 实现回归现有 `EmbeddingMathTest`；Qdrant 实现需容器/实例 |

### 6.4 回滚与验收

- 回滚：把知识库 `vector_store` 改回 `mysql`，向量数据仍在 MySQL 分片里（不删原列）。
- 验收：10 万级分片下检索 P95 < 300ms（目标值可按部署机器调整）；召回结果与 MySQL 实现 TopK 重合度抽检。

预估：接口抽象 + MySQL 实现 1 天；Qdrant 实现与联调 2–3 天（需要实例）。

---

## 7. 权限粒度细化（页面与权限键一一对应）

### 7.1 现状问题

`/device-group`、`/theme-resource`、`/plugin`、`/memory/chat`、`/portal` 等页面复用其他菜单的权限键（如 `system:device`、`system:role`、`system:config`、`system:dashboard`），授权时连带可见性不可控。

### 7.2 方案（一次做一类，不要大爆炸式改）

| 步骤 | 改动 |
|---|---|
| 1 | 为每个复用页新增独立权限键（menu + api 两条），如 `system:device-group`、`system:theme-resource`、`system:plugin`、`system:memory-chat`、`system:portal` |
| 2 | `db/init.sql` + `db/upgrade/<日期>__permission_split.sql`：INSERT IGNORE 新行 + 给管理员授权；**旧键保留**（不删，避免存量角色瞬间 403） |
| 3 | 后端 Controller：`@SaCheckPermission` 改新键（或 `@SaCheckPermission(value={"新键","旧键"}, mode=OR)` 灰度） |
| 4 | 前端路由 `meta.permission` 改新键；i18n 菜单标题不变 |
| 5 | 清理：确认所有部署都重存过权限后，再排期删旧键（单独升级脚本） |

### 7.3 验收与风险

- `PermissionKeyContractArchTest` 必须全绿（新键在 init.sql 且已授权管理员）；
- 风险：存量角色只授了旧键 → 灰度期用 OR 模式兼容；变更后提示用户清 Redis 权限缓存或后台重存。
- 验收：分别用「仅有旧键」「仅有新键」「管理员」三种角色验证页面可见性与接口 200/403。

预估：每个页面 2–3 小时；建议一次做 2–3 个页面。

---

## 8. 运维脚本归档（根目录 53 个一次性 `.sh`）

### 8.1 现状

根目录混放正式脚本（`deploy.sh`、`xiaozhi.sh`）与历史排障脚本（`deploy-621*.sh`、`check*.sh`、`fix-emqx*.sh`、`chk-*.sh`、`poll.sh` 等），有误执行风险；`AGENTS.md` 已规定发布只走 `deploy.sh`。

### 8.2 方案

1. `git mv` 历史脚本到 `scripts/legacy/`，并加 `scripts/legacy/README.md`（每个脚本一句话用途 + 「仅历史参考，不要用于发布」）；
2. 根目录只保留 `deploy.sh`（发布）、`xiaozhi.sh`（源码开发）、`deploy.sh` 依赖的辅助文件；
3. 新增 CI 检查（轻量）：`backend.yml` 加一步「根目录 `*.sh` 白名单校验」，防止再堆回来；
4. 不要删除脚本（保留追溯），只移动 + 标注。

### 8.3 验收

- `ls *.sh` 只剩白名单；`./deploy.sh --dry-run` 与 `./xiaozhi.sh status` 行为不变；
- 文档中引用旧脚本路径的地方（如有）同步更新。

预估：0.5 天（含全仓路径引用检查）。

---

## 9. 优先级建议

| 优先级 | 事项 | 理由 | 依赖 |
|---|---|---|---|
| P0 | §8 脚本归档、§7 权限粒度（2–3 页） | 纯工程卫生，风险低、收益立竿见影 | 无 |
| P1 | §1 TTS 真流式（阿里 → 腾讯/讯飞）、§2 STT 双向流式 | 直接兑现「极速响应」卖点；架构底座已就绪 | 厂商凭据 |
| P2 | §3 音色克隆、§6 向量库 | 商业卖点/规模扩展；都需要外部资源 | 厂商账号/向量库实例 |
| P3 | §4 插件 L1、§5 外部 MCP Server | 生态能力，需求方明确后再做 | 无/第三方 server |

> 排期原则沿用 `docs/PLATFORM-ROADMAP.md`：先建底座，再做能力，最后做界面；每项必须带「建表 → 后端 → 前端 → 测试」完整切片并通过架构守卫。

---

## 10. 通用约束（改这些缺口时不要破坏）

1. **播放器 60ms/帧节拍与打断逻辑**：只换音频来源，不动节拍（AGENTS.md）。
2. **Persona 为空必须重建**（`ensurePersona`），不要丢弃用户这一轮。
3. **数据库变更双轨**：`db/init.sql` + `db/upgrade/<日期>__<说明>.sql`，升级脚本幂等、只加不删。
4. **新增 `@SaCheckPermission` 必须同步 `db/init.sql` 并授权管理员**，否则 `PermissionKeyContractArchTest` 红。
5. **时间只走 `DateUtils`**，模型字段用 `xxxTime`。
6. **server 不 import dal/第三方 SDK**（`ModuleBoundaryArchTest`）；Controller 出参用 `ApiResponse<Resp>`，禁止裸 Map（`ServiceLayerArchTest`）。
