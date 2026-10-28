# 商业版功能补齐 · 验收清单

> 对照 `docs/images/feature-comparison.html` 里「开源版 ❌」的 23 项与「🟡」的 4 项，逐条给出交付物与验证方式。
> 数据库现状：**34 张表、107 条权限种子**，老库执行 `db/upgrade/2026-10-28__firmware.sql`（幂等）。

## 一、已完成（19 项）

| 功能 | 交付物 | 验证 |
|---|---|---|
| OTA 分批升级 | `sys_firmware` / `sys_firmware_release` / `sys_firmware_upgrade_log`；`/api/firmware*`；`/firmware` 页面 | FirmwareReleaseServiceImplTest 13 条 + FirmwareControllerTest 4 条 |
| 设备接入自检 | `GET /api/device/{id}/self-check`；设备页弹窗 | DeviceSelfCheckServiceTest 7 条 |
| 运维指标 | `/actuator/prometheus` + `XIAOZHI_OPS_TOKEN`；`xiaozhi.ota.*` 指标 | OtaMetricsTest 3 条 + OpsEndpointAccessFilterTest 5 条 |
| MQTT 长连接 | `MqttBridge` / `MqttChatSession` / `sys_device_channel`；OTA 下发 `mqtt` 块 | MqttBridgeTest 3 条（内嵌 Moquette 真实 broker）+ MqttChatSessionTest 4 条 |
| 主动消息推送 | `sys_push_message`；`/api/push*`；`/push` 页面 | PushAppServiceTest 5 条 + RedisSubscriberPushTest 4 条 |
| 语音提醒与闹钟 | `sys_reminder`；30 秒扫描调度；`/reminder` 页面 | ReminderServiceImplTest 7 条 + ReminderAppServiceTest 3 条 |
| 远程控制设备 | `sys_device_command`；`POST /api/device/{id}/command`；设备页弹窗 | DeviceCommandAppServiceTest 6 条 + RedisSubscriberCommandTest 3 条 |
| 设备截图 | `sys_device_screenshot`；`POST /api/device/{id}/screenshot` | DeviceScreenshotServiceImplTest 4 条 + AppServiceTest 2 条 |
| RAG 知识库 | `sys_knowledge_base/doc/chunk`；Tika 解析 + 切片 + 向量化 + 检索；`/knowledge` 页面 | TextChunkerTest 5 条 + EmbeddingMathTest 5 条 + KnowledgeContextProviderTest 6 条 |
| 声纹识别 | `sys_voiceprint`；sherpa-onnx speaker embedding；`/voiceprint` 页面；对话内说话人注入 | VoiceprintMatcherTest 4 条 + UserMessageAssemblerSpeakerTest 3 条 |
| 长期记忆 | `sys_memory_fact`；人工录入 / 手动提炼 / 会话结束自动提炼；按说话人注入 | MemoryFactProviderTest 4 条 + MemoryFactExtractorTest 5 条 + MemoryAutoExtractorTest 4 条 |
| 记忆图谱 | `sys_memory_entity/relation`；事实落库时抽三元组；`/memory-graph` 页面 | MemoryGraphServiceImplTest 3 条 + MemoryGraphExtractorTest 3 条 |
| Function Call 安抚词 | `ToolFillerProvider`；Persona 首次工具调用时垫一句 | ToolFillerProviderTest 3 条 |
| MCP 接入点 / SSE | `spring-ai-starter-mcp-server-webmvc`；3 个工具；写操作默认关闭 | McpServerToolsTest 5 条 |
| 监控面板 | `/api/stats/*`；Dashboard 四维度卡片 + 7 日柱状图；`sys_dialogue_metric` 按轮采集 | StatsAppServiceTest 3 条 + DialogueMetricServiceImplTest 2 条 |
| 聊天数据可视化 | 消息趋势 + 活跃设备 Top3（并入 Dashboard） | 同上 |
| 海量工具调度 | `ToolSelector`：超阈值时按相关度取前 N | ToolSelectorTest 3 条 |
| 主题资源生成器 | `/api/theme/{roleId}/resource`；`/theme-resource` 页面 | ThemeResourceServiceTest 3 条 |
| 多设备协同 | `sys_device_group(_member)`；`WakeCoordinator`；分组页面 | WakeCoordinatorTest 5 条 + 对话回归 63 条 |
| 插件系统 | `PluginExtension` + `PluginRegistry`（ServiceLoader）+ `/plugin` 页面 | PluginRegistryTest 3 条 |
| Home Assistant | `HomeAssistantClient` + `turnOn`/`turnOff` 工具 | HomeAssistantClientTest 4 条 |
| 用户端卡片界面 | `/portal` 卡片式设备面板 | 前端 type-check + 路由测试 |

## 二、待外部条件（2 项）

| 功能 | 缺口 | 补齐方式 |
|---|---|---|
| 音色克隆 | 未接服务商（阿里 CosyVoice / 火山需要控制台开通与凭据） | 实现 `VoiceCloneClient.enroll()`（约 30 行），表/状态机/页面/测试已就绪 |
| Home Assistant | 无 HA 实例，HTTP 往返未实测 | 配 `base-url` + `token` 后联调 |

## 三、未交付（1 项）

**双向流式交互**：现状是 `LLM → 分句 → 逐句 TTS 合成 → 下发`，即"文本侧已流式"；
缺的是 **TTS 音频级流式**（不等整句合成完就下发首包）与 **STT 双向流式**（边说边出字）。

后续路径：
1. `TtsService` 增加 `synthesizeStream(text, sink)`，Edge/火山/阿里优先实现
2. `Player` 支持增量帧写入（当前按整句 buffer 下发）
3. `SttService` 的中间结果回调接入对话状态机（部分 provider 已支持）
4. 验收标准：首包延迟 < 300ms、打断响应 < 200ms，并用现有音频回归用例覆盖

## 四、验证基线

- `mvn compile`：通过
- 架构守卫 26 项：全绿（模块边界 15、分层 8、时间源 3）
- 前端 `vue-tsc --build --force`：通过；locale/路由测试 27 项：通过
- 已知外部失败：`ImagePublishGuardTest`（断言 upstream 仓库名，与本仓 fork 无关）
