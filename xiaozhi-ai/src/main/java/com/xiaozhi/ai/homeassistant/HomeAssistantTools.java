package com.xiaozhi.ai.homeassistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/** 暴露给模型的 Home Assistant 工具：开/关设备。 */
@Slf4j
@Component
public class HomeAssistantTools {

    @Resource
    private HomeAssistantClient homeAssistantClient;

    @Tool(description = "打开一个智能家居设备，入参为 Home Assistant 实体ID，如 light.living_room")
    public String turnOn(String entityId) {
        return call("homeassistant", "turn_on", entityId);
    }

    @Tool(description = "关闭一个智能家居设备，入参为 Home Assistant 实体ID，如 light.living_room")
    public String turnOff(String entityId) {
        return call("homeassistant", "turn_off", entityId);
    }

    private String call(String domain, String service, String entityId) {
        try {
            boolean ok = homeAssistantClient.callService(domain, service, entityId);
            return ok ? "已执行" : "Home Assistant 拒绝了请求";
        } catch (RuntimeException e) {
            log.warn("Home Assistant 调用失败 entityId={}", entityId, e);
            return "调用失败：" + e.getMessage();
        }
    }
}