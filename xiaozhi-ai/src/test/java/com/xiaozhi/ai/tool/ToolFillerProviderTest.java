package com.xiaozhi.ai.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolFillerProviderTest {

    @Test
    void picksOneOfConfiguredPhrases() {
        ToolFillerProvider provider = new ToolFillerProvider();
        // 逗号也是分隔符，文案里不能带逗号，否则会被拆成两句
        provider.setConfigured("稍等我查一下,我想想,马上好");

        assertThat(provider.enabled()).isTrue();
        assertThat(provider.next()).isIn("稍等我查一下", "我想想", "马上好");
    }

    @Test
    void blankConfigDisablesFiller() {
        ToolFillerProvider provider = new ToolFillerProvider();
        provider.setConfigured("  ");

        assertThat(provider.enabled()).isFalse();
        assertThat(provider.next()).isNull();
    }

    @Test
    void onlySeparatorsYieldNull() {
        ToolFillerProvider provider = new ToolFillerProvider();
        provider.setConfigured(",,");

        assertThat(provider.next()).isNull();
    }
}