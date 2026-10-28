package com.xiaozhi.ai.plugin;

/**
 * 插件扩展点。实现类放在 classpath 上并通过 META-INF/services 声明即可被发现，无需改主程序。
 * <p>
 * 当前版本只做「发现 + 启停」：插件注册的工具/能力由各自的 Spring Bean 承载，
 * 启停开关用于在运行时屏蔽某个插件，不影响其它插件。
 */
public interface PluginExtension {

    /** 全局唯一标识，建议用 反向域名 形式 */
    String id();

    /** 展示名 */
    String name();

    default String version() {
        return "1.0.0";
    }

    default String description() {
        return "";
    }
}