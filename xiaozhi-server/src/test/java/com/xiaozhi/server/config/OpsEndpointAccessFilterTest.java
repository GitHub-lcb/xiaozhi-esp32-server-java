package com.xiaozhi.server.config;

import com.xiaozhi.common.config.OpsEndpointAccessConfig.OpsEndpointAccessFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 运维端点的令牌闸门：只有配了令牌、只有只读的指标/健康端点、只有令牌对得上时才放行。
 * 配错的表现是把 /actuator/prometheus 暴露给全网，或者 Prometheus 永远抓不到数据。
 */
class OpsEndpointAccessFilterTest {

    private static final String TOKEN = "ops-secret";

    @Test
    void rejectsWhenTokenIsNotConfigured() throws Exception {
        OpsEndpointAccessFilter filter = new OpsEndpointAccessFilter("");
        MockHttpServletRequest request = request("/actuator/prometheus");
        request.addHeader("X-Ops-Token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsWrongToken() throws Exception {
        OpsEndpointAccessFilter filter = new OpsEndpointAccessFilter(TOKEN);
        MockHttpServletRequest request = request("/actuator/prometheus");
        request.addHeader("X-Ops-Token", "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void acceptsTokenOnPrometheusEndpoint() throws Exception {
        OpsEndpointAccessFilter filter = new OpsEndpointAccessFilter(TOKEN);
        MockHttpServletRequest request = request("/actuator/prometheus");
        request.addHeader("X-Ops-Token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void acceptsBearerToken() throws Exception {
        OpsEndpointAccessFilter filter = new OpsEndpointAccessFilter(TOKEN);
        MockHttpServletRequest request = request("/actuator/health");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenDoesNotUnlockOtherActuatorEndpoints() throws Exception {
        OpsEndpointAccessFilter filter = new OpsEndpointAccessFilter(TOKEN);
        MockHttpServletRequest request = request("/actuator/env");
        request.addHeader("X-Ops-Token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }
}
