package com.xiaozhi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dialogue 独立启动入口
 * <p>
 * 包含：WebSocket/MQTT。
 * 可横向扩展，通过 Redis Pub/Sub 与其他实例协作。
 * <p>
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@ComponentScan(
    basePackages = {
        // xiaozhi-common
        "com.xiaozhi.common",
        "com.xiaozhi.communication",
        "com.xiaozhi.utils",
        // xiaozhi-service (dialogue 需要的部分)
        "com.xiaozhi.channel",
        "com.xiaozhi.command",
        "com.xiaozhi.knowledge",
        "com.xiaozhi.screenshot",
        "com.xiaozhi.config",
        "com.xiaozhi.storage",
        "com.xiaozhi.device",
        "com.xiaozhi.devicegroup",
        "com.xiaozhi.mcptoolexclude",
        "com.xiaozhi.memoryfact",
        "com.xiaozhi.memorygraph",
        "com.xiaozhi.message",
        "com.xiaozhi.monitoring",
        "com.xiaozhi.role",
        "com.xiaozhi.stats",
        "com.xiaozhi.summary",
        "com.xiaozhi.voiceprint",
        "com.xiaozhi.task",
        "com.xiaozhi.verifycode",
        // xiaozhi-ai
        "com.xiaozhi.ai",
        // xiaozhi-dialogue
        "com.xiaozhi.dialogue",
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
    }
)
@MapperScan({
    "com.xiaozhi.channel.dal.mysql.mapper",
    "com.xiaozhi.command.dal.mysql.mapper",
    "com.xiaozhi.knowledge.dal.mysql.mapper",
    "com.xiaozhi.screenshot.dal.mysql.mapper",
    "com.xiaozhi.config.dal.mysql.mapper",
    "com.xiaozhi.device.dal.mysql.mapper",
    "com.xiaozhi.devicegroup.dal.mysql.mapper",
    "com.xiaozhi.mcptoolexclude.dal.mysql.mapper",
    "com.xiaozhi.memoryfact.dal.mysql.mapper",
    "com.xiaozhi.memorygraph.dal.mysql.mapper",
    "com.xiaozhi.message.dal.mysql.mapper",
    "com.xiaozhi.role.dal.mysql.mapper",
    "com.xiaozhi.stats.dal.mysql.mapper",
    "com.xiaozhi.summary.dal.mysql.mapper",
    "com.xiaozhi.voiceprint.dal.mysql.mapper",
    "com.xiaozhi.verifycode.dal.mysql.mapper",
})
public class DialogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DialogueApplication.class, args);
    }
}
