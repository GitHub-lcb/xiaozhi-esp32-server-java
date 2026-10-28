package com.xiaozhi.ai.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 注册表的语义：默认启用、可停用、停用后 isEnabled 为 false、未知插件不能被“停用成功”。 */
class PluginRegistryTest {

    @Test
    void registeredPluginIsEnabledByDefault() {
        PluginRegistry registry = new PluginRegistry();
        registry.register(plugin("demo", "示例插件"));

        List<Map<String, Object>> list = registry.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("enabled")).isEqualTo(true);
        assertThat(registry.isEnabled("demo")).isTrue();
    }

    @Test
    void toggleOffThenOn() {
        PluginRegistry registry = new PluginRegistry();
        registry.register(plugin("demo", "示例插件"));

        assertThat(registry.setEnabled("demo", false)).isTrue();
        assertThat(registry.isEnabled("demo")).isFalse();
        assertThat(registry.setEnabled("demo", true)).isTrue();
        assertThat(registry.isEnabled("demo")).isTrue();
    }

    @Test
    void unknownPluginCannotBeToggled() {
        PluginRegistry registry = new PluginRegistry();

        assertThat(registry.setEnabled("missing", false)).isFalse();
        assertThat(registry.isEnabled("missing")).isFalse();
    }

    private PluginExtension plugin(String id, String name) {
        return new PluginExtension() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}