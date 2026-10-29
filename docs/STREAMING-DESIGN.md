# 双向流式交互 · 设计与落地计划

## 1. 现状（实测结论，非推测）

链路现在是：

```
LLM token 流 → SentenceHelper 分句 → TtsService.textToSpeech(整句) → 落 wav 文件
     → OpusProcessor 编码 → 帧队列 → Player 按 60ms 节拍下发
```

**已经在流式的部分**：
- LLM 侧逐 token 流式（`chatModel.stream`）
- 分句侧本来就边收边切：命中 `。！？` 立即切；没命中时累计到 `MIN_SENTENCE_LENGTH = 8` 字后，遇到逗号/顿号/换行/表情也会切（`SentenceHelper.take`）
- 下发侧按 60ms 节拍连续发帧，不等整段生成完

**真正缺的两块**：
1. **TTS 音频级流式**：`TtsService` 只有一个方法 `Path textToSpeech(String text)`，返回**完整文件路径**。必须等整句合成完、写完文件，才开始编码下发。首包延迟 = 整句合成时延。
2. **STT 双向流式**：识别侧是"收完整段再出结果"，没有边说边出中间结果。

## 2. 目标与验收指标

| 指标 | 现状（估） | 目标 |
|---|---|---|
| 首包延迟（用户说完 → 设备出声） | 1.5–2.5s | **< 800ms**（云 TTS 流式）/ < 1.2s（本地） |
| 打断响应（用户插话 → 停止播放） | < 200ms | 保持 |
| 长句首字时间 | 等整句（可能 3–5s） | **< 300ms** 出首帧 |

## 3. 设计

### 3.1 新增能力接口（不破坏现有 provider）

```java
public interface StreamingTtsService extends TtsService {
    /** 边合成边返回 PCM 分块；调用方拿到一块即可编码下发 */
    Flux<byte[]> synthesizeStream(String text);

    /** 该 provider 是否真的流式（false 时上层走伪流式适配） */
    default boolean nativeStreaming() { return true; }
}
```

- **未改造的 provider** 走 `FileBackedStreamingAdapter`：先调旧方法拿文件，再按 4KB 分块吐 Flux。链路统一，但首包不变快——这是"能上线、可回归"的过渡形态。
- **火山双向流式 / 阿里 CosyVoice 流式** 实现 `nativeStreaming() = true`，真正边收边编。

### 3.2 Synthesizer 改造

```java
// 现在：synthesize(Flux<String> textFlux) → 每个 sentence 调 textToSpeech → 整文件编码
// 之后：每句若支持流式，则订阅 synthesizeStream，边收 PCM 边 encode + enqueue
```

关键点：`OpusProcessor` 已经支持逐帧编码（`flushLeftover` 处理残帧），只需要把"读完整文件"换成"喂 chunk"。**Player 完全不用改**——它本来就按队列节拍发。

### 3.3 STT 双向流式（第二阶段）

`SttService` 增加 `Flux<SttResult> stream(...)`，火山/阿里 websocket 实时识别接进来；`DialogueService` 在中间结果里做"提前打断判定"（现有 barge-in 逻辑可复用）。

## 4. 分阶段落地

| 阶段 | 内容 | 依赖 | 预估 |
|---|---|---|---|
| P1 | `StreamingTtsService` + `FileBackedStreamingAdapter` + Synthesizer 改造 + 回归测试 | 无（可离线验证） | 2 天 |
| P2 | 火山/阿里真实流式实现（各自的 websocket 协议） | **需要 API Key** 联调 | 2–3 天 |
| P3 | STT 双向流式 | **需要 API Key** | 2 天 |
| P4 | 指标埋点：首包时延、流式中断率，进 `sys_dialogue_metric` | 无 | 0.5 天 |

## 5. 风险

| 风险 | 缓解 |
|---|---|
| 音频链路回归（63 条对话用例依赖时序） | P1 只改"音频来源"，不改发送节拍；先用伪流式适配器跑全量回归，确认零行为变化再上真实流式 |
| 云厂商流式协议不稳定/断流 | 每个 chunk 加超时；断流回退到旧的整句合成 |
| 采样率/声道不一致导致杂音 | 流式实现必须统一 16k 单声道 16bit，加一条断言 |
| 打断时流式回调仍在写 | 复用现有 `generation` 代次机制，回调里校验代次 |

## 6. 验收方式

1. **离线可验**：P1 完成后，用 `FileBackedStreamingAdapter` 跑全量对话用例，行为应与改造前完全一致（帧数、时长、顺序）
2. **指标可验**：P4 埋点后在监控面板看"首包时延 P50/P95"是否下降
3. **真机可验**：用 ESP32 设备实测"一句话问完到出声"的体感时延
