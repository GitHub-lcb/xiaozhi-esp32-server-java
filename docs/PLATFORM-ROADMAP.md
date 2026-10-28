# 商业版能力补齐路线图

> 目标：把 README / 功能对比表里标为「商业版」的能力，在本仓库（开源版代码）里全部实现。
> 口径：每个功能都要交付「建表 → 后端 → 前端 → 测试」的完整切片，并且本地编译、单测通过才算完成。

## 1. 差距清单

[`docs/images/feature-comparison.html`](../docs/images/feature-comparison.html) 里开源版标 ❌ 的 23 项、标 🟡 的 4 项：

| 分组 | 待实现功能 |
|------|-----------|
| 通信 | MQTT 长连接 / 主动唤醒、主动消息推送、双向流式交互 |
| 设备 | OTA 分批升级（时间窗/升级记录）、多设备就近应答、设备截图、主题资源生成器、设备接入一键自检 |
| 语音 | 声纹识别、音色克隆、极速响应、Function Call 安抚词 |
| AI | 知识库 RAG、长期记忆（关键信息提取）、多成员独立记忆、记忆图谱、海量工具智能调度 |
| 平台 | MCP 接入点 / SSE、语音提醒与闹钟、插件系统与热加载、Home Assistant、多设备协同、远程控制 |
| 可视化 | 监控面板（Token/时延/活跃度）、聊天数据可视化、运维指标对接、用户端原生卡片界面 |

## 2. 分期计划

分期原则：**先建底座，再做能力，最后做界面**。后一期的功能复用前一期的表、事件与接口。

### 第一期 · 设备固件与运维（已完成）

| 功能 | 交付物 | 状态 |
|------|--------|------|
| OTA 分批升级 | `sys_firmware` / `sys_firmware_release` / `sys_firmware_upgrade_log`；灰度策略（全量/百分比/白名单）；时间窗；升级记录；`/api/firmware*` 三组接口；`/firmware` 后台页面 | ✅ |
| 设备接入一键自检 | `GET /api/device/{deviceId}/self-check`：绑定、在线、信息完整度、OTA 地址、实时通道、设备鉴权、可升级固件七项，逐项给依据与建议；设备页「接入自检」弹窗 | ✅ |
| 运维指标对接 | `/actuator/health`、`/actuator/prometheus`；`xiaozhi.ota.check`、`xiaozhi.ota.upgrade.delivered`、`xiaozhi.dialogue.servers`；`XIAOZHI_OPS_TOKEN` 令牌只放行两个只读端点 | ✅ |

第一期验收：`mvn compile` 通过；新增 24 条单测（OTA 决策 13、控制器 4、自检 7、指标 3、运维闸门 5）全绿；
架构守卫（模块边界、分层、权限键、组件扫描、时间源等）全绿。

### 第二期 · 实时通道

| 功能 | 依赖 |
|------|------|
| MQTT 长连接 / 主动唤醒 | ✅ 底座已落地：`sys_device_channel` 通道表、`MqttBridge`（Paho 客户端 + 订阅设备主题 + 复用 `MessageHandler` 对话管线）、`MqttChatSession`（JSON 主题 / base64 音频）、在线巡检、OTA 下发 `mqtt` 配置块 |
| 主动消息推送 | ✅ `sys_push_message` + `/api/push*` 接口 + `/push` 后台页面；server 选目标/判在线/计数，dialogue 经 Redis 广播找到会话后用 `Persona.speakNow` 直接合成播报（不过 LLM）；离线设备跳过不排队 |
| 语音提醒与闹钟 | ✅ `sys_reminder` + `/api/reminder*` + `/reminder` 页面；server 每 30 秒扫描到点提醒，在线设备经 Redis 广播播报，离线跳过一次（重复任务顺延、一次性记「已错过」）；重复规则支持每天/工作日并跳过停机期间已过去的时刻 |
| 多设备就近应答 | 🟡 `sys_device_group` / `sys_device_group_member` 建表 + `WakeCoordinator`（Redis SET NX EX 原子占位，默认 5 秒窗口，Redis 故障时放行）；待接：`DialogueService.handleWakeWord` 里按分组判定 + 分组管理页面 |
| 远程控制设备 | ✅ `sys_device_command` + `POST /api/device/{id}/command` + 设备页「远程控制」弹窗；动作翻译成设备 MCP 工具调用（默认 `self.reboot` / `self.audio_speaker.set_volume`，可配），经 Redis 交给持有会话的 dialogue 执行并回填结果；离线设备在下发前拒绝 |
| 设备截图 | ✅ `sys_device_screenshot` + `POST /api/device/{id}/screenshot`；调用设备截图工具（默认 `self.screen.snapshot`，可配），回传的 http/data-URI 地址由 dialogue 回填，设备页「截图」弹窗预览；纯 base64 不落库，避免大字段撑爆表 |
| 双向流式交互 | TTS/STT provider 增加 stream 接口，火山/阿里/讯飞优先 |

### 第三期 · AI 能力

| 功能 | 依赖 |
|------|------|
| 知识库 RAG | ✅ 已完成：对话按角色自动检索并注入（`KnowledgeContextProvider` → 系统提示词，失败降级不影响对话）+ `/knowledge` 后台页面（库/文档/检索测试）。后端：`sys_knowledge_base/doc/chunk` 三张表、Tika 解析（PDF/Office/HTML/纯文本）、切片（500 字 + 50 重叠）、向量化入库、余弦检索与 `/api/knowledge*` 接口（含检索测试）；待做：对话挂载（按 roleId 注入提示词）与后台页面 |
| 长期记忆 / 多成员独立记忆 | ✅ 已完成：人工录入 + 手动提炼 + **会话结束自动提炼**（`xiaozhi.memory.auto-extract-enabled`，默认关） + 按说话人注入。明细：`sys_memory_fact` 表、`/api/memory-fact` 接口、`/memory-fact` 页面、按「说话人 + 全家」注入提示词（声纹识别决定说话人）；待做：对话结束后自动抽取事实（`source=auto`） |
| 记忆图谱 | ✅ `sys_memory_entity` / `sys_memory_relation`；事实落库时按关系词表自动抽三元组（人工/自动两条路径都接），关系随记忆注入对话，`/memory-graph` 页面可查看与手工维护 |
| 声纹识别 | 🟡 已就绪：`sys_voiceprint` + sherpa-onnx `SpeakerEmbeddingExtractor` 注册/比对 + `/voiceprint` 页面（注册、识别测试）；待做：对话链路里自动标注说话人 |
| 音色克隆 | 火山/阿里云克隆 API + `sys_voice_clone` + 文件上传 |
| 海量工具智能调度 | ✅ `ToolSelector`：工具数超过 `xiaozhi.tools.max-count`（默认 12）时，按本轮问题与工具名/描述的字符重合度粗排取前 N；工具少时原样下发，零扰动 |
| Function Call 安抚词 | 工具调用期间播放填充语 |

### 第四期 · 平台与生态

Home Assistant（✅ `HomeAssistantClient` + `turnOn`/`turnOff` 两个工具，经 MCP 接入点暴露；未配置时明确报错） / MCP 接入点 / SSE（`spring-ai-starter-mcp-server-webmvc`，✅ 已暴露 `listDevices` / `deviceStatus` / `sendNotification` 三个工具，写操作默认关闭）、插件系统与热加载、Home Assistant 集成、多设备协同（设备间状态感知）。

### 第五期 · 前端与可视化

监控面板（Token/时延/活跃度）✅、聊天数据可视化 ✅、设备主题资源生成器 ✅（`/theme-resource` 生成并下载 JSON 契约）、用户端原生卡片界面 ⬜。

> **进度**：监控面板已完整落地——活跃度、消息量、**Token 消耗**、**平均时延**、活跃设备 Top3，
> 数据来自 `/api/stats/*`（对话维度由 `sys_dialogue_metric` 按轮次采集）。聊天数据可视化的趋势与 Top 设备已并入 Dashboard。

## 3. 第一期详细设计：OTA 分批升级

### 3.1 数据模型

```
sys_firmware                 固件版本库（version + chipModel 唯一）
sys_firmware_release         升级任务（灰度策略 / 时间窗 / 状态机 0待开始 1进行中 2已暂停 3已结束）
sys_firmware_upgrade_log     升级记录（设备、来源版本、目标版本、下发时间）
```

### 3.2 下发决策

设备 `POST /api/device/ota` 时：

1. 取 `state=1` 的任务，过滤时间窗（`startTime <= now <= endTime`，空表示不限）；
2. 过滤芯片型号（空表示不限）与目标固件 `state=1`；
3. 过滤版本：目标版本必须高于设备当前版本（语义化比较）；
4. 按策略放行：`all` 全量、`percent` 按 `deviceId` 哈希取模、`whitelist` 白名单；
5. 命中多台时取版本最高的一条，写 `sys_firmware_upgrade_log`，返回 `firmware.url/version`；
6. 未命中保持原行为（返回 OTA 检查地址与当前版本），不影响老设备。

### 3.3 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/api/firmware` | 固件版本 CRUD |
| GET/POST/PUT/DELETE | `/api/firmware/release` | 升级任务 CRUD |
| POST | `/api/firmware/release/{id}/state` | 发布 / 暂停 / 结束 |
| GET | `/api/firmware/upgrade-log` | 升级记录分页 |

### 3.4 前端

`/firmware` 页面三个 Tab：固件版本、升级任务、升级记录；菜单权限 `system:firmware`。

## 4. 开发约定

- **JDK 21**：`E:\develop-lcb\env\java\jdk21`（默认 `java` 是 1.7，不能用）
- **Maven**：`E:\develop-lcb\env\apache-maven-3.9.11\bin\mvn.cmd`，走内网 nexus
- 编译：`mvn -B compile`；单模块测试：`mvn -B -pl xiaozhi-server -am test -Dtest=XxxTest`
- 建表与权限种子一律进 `db/init.sql`（新装即生效），老库用 `db/upgrade/` 下的增量脚本
- 新增 `@SaCheckPermission` 的键必须同时写进 `db/init.sql`，否则 `PermissionKeyContractArchTest` 失败
- 时间一律走 `DateUtils`，不要 `LocalDateTime.now()`

## 5. 进度

- [x] 路线图
- [x] 第一期：OTA 分批升级（`sys_firmware` / `sys_firmware_release` / `sys_firmware_upgrade_log` + 后台页面）
- [x] 第一期：设备接入一键自检（`GET /api/device/{id}/self-check` + 设备页「接入自检」）
- [x] 第一期：运维指标对接（`/actuator/prometheus` + `XIAOZHI_OPS_TOKEN` 令牌放行 + OTA/实例数指标）
