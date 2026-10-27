package com.xiaozhi.ai.llm.factory.providers;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * DeepSeek 模型提供者。
 * <p>
 * 协议兼容 OpenAI，故复用 {@link OpenAiModelProvider} 的全部请求构建逻辑，
 * 只覆盖思考模式的表达方式：
 * <ul>
 *     <li>DeepSeek 的 {@code thinking.type} <b>默认 enabled</b>。父类"关闭思考就不下发
 *         {@code reasoning_effort}"的做法对它无效——省略参数等于继续思考，接口不报错，
 *         只表现为首字延迟变大、正文被 reasoning token 挤占；</li>
 *     <li>{@code reasoning_effort} 支持 {@code none / low / high / max}，其中
 *         {@code minimal} 映射为 {@code low}、{@code medium} 映射为 {@code high}。
 *         父类默认的 {@code medium} 在 DeepSeek 上等同 {@code high}，语音对话等不起，改用 {@code low}。</li>
 * </ul>
 */
@Slf4j
@Component
public class DeepSeekModelProvider extends OpenAiModelProvider {

    @Override
    public String getProviderName() {
        return "deepseek";
    }

    @Override
    protected void applyThinkingOptions(OpenAiChatOptions.Builder builder, boolean enableThinking, String model) {
        // 关闭必须显式 none：DeepSeek 默认就在思考，省略参数关不掉
        String effort = enableThinking ? "low" : "none";
        builder.reasoningEffort(effort);
        log.info("DeepSeek model {} 思考模式={}, reasoningEffort={}", model, enableThinking, effort);
    }
}
