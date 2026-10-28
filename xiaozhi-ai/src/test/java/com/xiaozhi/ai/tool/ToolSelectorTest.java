package com.xiaozhi.ai.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 召回的两条底线：工具少的时候一个都不能少（不能引入扰动），工具多的时候相关的必须排在前面。
 */
class ToolSelectorTest {

    private final ToolSelector selector = new ToolSelector();

    @Test
    void smallToolSetPassesThroughUnchanged() {
        List<ToolCallback> tools = List.of(tool("time", "当前时间"), tool("weather", "天气查询"));

        assertThat(selector.select(tools, "今天天气怎么样")).isEqualTo(tools);
    }

    @Test
    void mostRelevantToolIsKeptWhenTrimming() {
        java.util.List<ToolCallback> tools = new java.util.ArrayList<>();
        tools.add(tool("weather", "查询天气"));
        for (int i = 0; i < 20; i++) {
            tools.add(tool("tool" + i, "无关工具" + i));
        }
        org.springframework.test.util.ReflectionTestUtils.setField(selector, "maxCount", 5);

        List<ToolCallback> selected = selector.select(tools, "今天天气如何");

        assertThat(selected).hasSize(5);
        assertThat(selected.get(0).getToolDefinition().name()).isEqualTo("weather");
    }

    @Test
    void blankQueryKeepsEverything() {
        java.util.List<ToolCallback> tools = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tools.add(tool("tool" + i, "描述"));
        }
        org.springframework.test.util.ReflectionTestUtils.setField(selector, "maxCount", 5);

        assertThat(selector.select(tools, "  ")).hasSize(20);
    }

    private ToolCallback tool(String name, String description) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);
        when(definition.description()).thenReturn(description);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }
}