package com.xiaozhi.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钉住「可信代理」环境变量的名字契约。
 * <p>
 * {@link TrustedProxyPolicy} 绑定的是 {@code xiaozhi.security.trusted-proxies}。
 * 实测（本测试即是契约）：环境变量必须写成 {@code XIAOZHI_SECURITY_TRUSTED_PROXIES}
 * （每个点换成下划线，<b>连字符也写成下划线</b>）；写成去掉下划线的
 * {@code XIAOZHI_SECURITY_TRUSTEDPROXIES} 不会被绑定，限流/审计会静默退回
 * 「只认 TCP 对端地址」，而配置的人以为已经生效了。
 * <p>
 * docker-compose.yml / .env.example 里的变量名必须与本测试一致。
 */
class TrustedProxyEnvBindingTest {

    @Test
    void bindsCommaSeparatedTrustedProxiesFromEnvName() {
        TrustedProxyPolicy policy = bindWith(Map.of(
            "XIAOZHI_SECURITY_TRUSTED_PROXIES", "10.0.0.0/8,172.16.0.0/12"));

        assertThat(policy.getTrustedProxies())
            .as("XIAOZHI_SECURITY_TRUSTED_PROXIES 是实测可用的环境变量写法，必须能绑上列表")
            .containsExactly("10.0.0.0/8", "172.16.0.0/12");
    }

    @Test
    void dashRemovedVariantDoesNotBind() {
        TrustedProxyPolicy policy = bindWith(Map.of(
            "XIAOZHI_SECURITY_TRUSTEDPROXIES", "10.0.0.0/8"));

        assertThat(policy.getTrustedProxies())
            .as("XIAOZHI_SECURITY_TRUSTEDPROXIES 不会绑定 trusted-proxies，不能当作生效配置")
            .isEmpty();
    }

    private TrustedProxyPolicy bindWith(Map<String, Object> environmentVariables) {
        StandardEnvironment environment = new StandardEnvironment();
        // 放最前面：是否被 relaxed 绑定只看这个 source 的 name 映射规则
        environment.getPropertySources().addFirst(
            new SystemEnvironmentPropertySource("test-environment", environmentVariables));
        return Binder.get(environment)
            .bind("xiaozhi.security", Bindable.of(TrustedProxyPolicy.class))
            .orElseGet(TrustedProxyPolicy::new);
    }
}
