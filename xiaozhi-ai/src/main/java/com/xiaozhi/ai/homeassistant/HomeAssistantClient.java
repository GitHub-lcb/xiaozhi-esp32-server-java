package com.xiaozhi.ai.homeassistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Home Assistant 服务调用客户端。
 * <p>
 * 只做最常用的一件事：调用 HA 的 /api/services/{domain}/{service} 控制某个实体。
 * 没配置地址或令牌时 {@link #available()} 返回 false，工具会明确告诉模型「没有接入 HA」，
 * 而不是静默返回成功。
 */
@Slf4j
@Component
public class HomeAssistantClient {

    @Value("${xiaozhi.home-assistant.base-url:}")
    private String baseUrl;

    @Value("${xiaozhi.home-assistant.token:}")
    private String token;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public boolean available() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(token);
    }

    /**
     * 调用一次服务，例如 domain=light service=turn_on entityId=light.living_room。
     *
     * @return 是否被 HA 接受（2xx）
     */
    public boolean callService(String domain, String service, String entityId) {
        if (!available()) {
            throw new IllegalStateException("未配置 Home Assistant：请设置 xiaozhi.home-assistant.base-url 与 token");
        }
        if (!StringUtils.hasText(domain) || !StringUtils.hasText(service)) {
            throw new IllegalArgumentException("domain 与 service 不能为空");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(baseUrl) + "/api/services/" + domain + "/" + service))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(entityId)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用 Home Assistant 被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("调用 Home Assistant 失败：" + e.getMessage(), e);
        }
    }

    /** 请求体：HA 的 services 接口要求 entity_id 放在 body 里 */
    static String payload(String entityId) {
        if (!StringUtils.hasText(entityId)) {
            return "{}";
        }
        return "{\"entity_id\":\"" + entityId.trim() + "\"}";
    }

    private String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}