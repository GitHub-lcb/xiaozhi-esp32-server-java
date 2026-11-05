package com.xiaozhi.ai.llm.factory.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钉住 DeepSeek 相对 OpenAI 协议的思考模式差异。
 * <p>
 * DeepSeek 的 {@code thinking.type} 默认就是 enabled：省略 {@code reasoning_effort} 等于继续思考，
 * 接口层面完全不报错，回归后只表现为首字变慢、正文变短甚至为空。
 * 因此关闭思考必须显式下发 {@code none}；开启时未配置强度默认 {@code low}，
 * 因为 {@code medium} 会被 DeepSeek 映射成 {@code high}。
 */
class DeepSeekModelProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MODEL = "deepseek-flash";

    private JsonNode chatApiRequest(OpenAiChatOptions.Builder builder) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(builder.build()));
    }

    @Test
    void thinkingDisabledSendsNoneReasoningEffort() throws Exception {
        var builder = OpenAiChatOptions.builder().model(MODEL);

        new DeepSeekModelProvider().applyThinkingOptions(builder, false, MODEL, null);

        assertThat(chatApiRequest(builder).path("reasoning_effort").asText()).isEqualTo("none");
    }

    @Test
    void thinkingEnabledWithoutConfigDefaultsToLow() throws Exception {
        var builder = OpenAiChatOptions.builder().model(MODEL);

        new DeepSeekModelProvider().applyThinkingOptions(builder, true, MODEL, null);

        assertThat(chatApiRequest(builder).path("reasoning_effort").asText()).isEqualTo("low");
    }

    @Test
    void thinkingEnabledUsesConfiguredEffort() throws Exception {
        var builder = OpenAiChatOptions.builder().model(MODEL);

        new DeepSeekModelProvider().applyThinkingOptions(builder, true, MODEL, "high");

        assertThat(chatApiRequest(builder).path("reasoning_effort").asText()).isEqualTo("high");
    }

    @Test
    void thinkingEnabledMapsUnsupportedEffortsToDeepSeekValues() throws Exception {
        var minimalBuilder = OpenAiChatOptions.builder().model(MODEL);
        new DeepSeekModelProvider().applyThinkingOptions(minimalBuilder, true, MODEL, "minimal");
        assertThat(chatApiRequest(minimalBuilder).path("reasoning_effort").asText()).isEqualTo("low");

        var mediumBuilder = OpenAiChatOptions.builder().model(MODEL);
        new DeepSeekModelProvider().applyThinkingOptions(mediumBuilder, true, MODEL, "medium");
        assertThat(chatApiRequest(mediumBuilder).path("reasoning_effort").asText()).isEqualTo("high");
    }

    @Test
    void providerNameIsDeepseek() {
        assertThat(new DeepSeekModelProvider().getProviderName()).isEqualTo("deepseek");
    }
}
