package com.xiaozhi.ai.plugin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件注册表：ServiceLoader 发现 + 运行时启停。
 * <p>
 * 启停状态放在内存里：重启后回到「全部启用」。这是有意的——插件是代码的一部分，
 * 真正的下线应该是移除 jar，而不是靠一个会被重启清掉的开关。
 */
@Slf4j
@Component
public class PluginRegistry {

    private final Map<String, PluginExtension> plugins = new LinkedHashMap<>();
    private final Set<String> disabled = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void load() {
        try {
            for (PluginExtension plugin : ServiceLoader.load(PluginExtension.class)) {
                register(plugin);
            }
            log.info("已发现 {} 个插件扩展", plugins.size());
        } catch (RuntimeException e) {
            log.warn("插件加载失败，按无插件运行", e);
        }
    }

    public void register(PluginExtension plugin) {
        if (plugin == null || plugin.id() == null) {
            return;
        }
        plugins.put(plugin.id(), plugin);
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>(plugins.size());
        plugins.values().forEach(plugin -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", plugin.id());
            item.put("name", plugin.name());
            item.put("version", plugin.version());
            item.put("description", plugin.description());
            item.put("enabled", !disabled.contains(plugin.id()));
            result.add(item);
        });
        return result;
    }

    public boolean setEnabled(String pluginId, boolean enabled) {
        if (!plugins.containsKey(pluginId)) {
            return false;
        }
        if (enabled) {
            disabled.remove(pluginId);
        } else {
            disabled.add(pluginId);
        }
        return true;
    }

    public boolean isEnabled(String pluginId) {
        return plugins.containsKey(pluginId) && !disabled.contains(pluginId);
    }
}