-- 2026-11-04 StackChan 数据接口：设备舞蹈表 + App Store 应用表。
-- 背景：StackChan 固件/App 的跳舞数据与 App Store 列表从 Go 版后端迁移到 Java 版，新增两张独立业务表。
-- 适用：已经用旧版 db/init.sql 建好库的环境；新装环境直接执行 db/init.sql 即可，无需本脚本。
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-11-04__stackchan_dance_app.sql
-- 约定：CREATE TABLE IF NOT EXISTS，可重复执行；不 DROP 业务表、不动任何已有数据。
-- Windows 本机 mysql 客户端默认字符集可能是 gbk（服务端/容器内为 utf8mb4），
-- 不声明的话下面这些中文注释与 COMMENT 会报 ERROR 1366 Incorrect string value。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_stackchan_dance` (
  `danceId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '舞蹈ID',
  `mac` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备MAC，统一小写冒号格式',
  `danceName` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '舞蹈名称',
  `danceData` longtext COLLATE utf8mb4_unicode_ci COMMENT '舞蹈动作数据，JSON 数组原文',
  `musicUrl` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '背景音乐地址',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`danceId`),
  KEY `idx_stackchan_dance_mac` (`mac`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan 设备舞蹈表';

CREATE TABLE IF NOT EXISTS `sys_stackchan_app` (
  `appId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '应用ID',
  `appName` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用名称',
  `appIconUrl` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用图标地址',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '应用描述',
  `firmwareUrl` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '固件/安装包下载地址',
  `state` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1上架 0下架',
  `isDeleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删除：0正常 1已删除',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appId`),
  KEY `idx_stackchan_app_state` (`isDeleted`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan App Store 应用表';
