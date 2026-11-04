-- 2026-11-04 StackChan 社区模块：帖子 + 帖子评论 + 全景图 + 好友关系。
-- 背景：Go 版 StackChan 后端的 api/post、api/pano、api/friend 三组接口在 Java 版补齐，新增四张独立业务表。
-- 适用：已经用旧版 db/init.sql 建好库的环境；新装环境直接执行 db/init.sql 即可，无需本脚本。
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-11-04__stackchan_community.sql
-- 约定：CREATE TABLE IF NOT EXISTS，可重复执行；不 DROP 业务表、不动任何已有数据。
-- 列名与 Java DO 字段同名（驼峰），时间列统一 createTime/updateTime，mac 统一小写冒号格式。
-- Windows 本机 mysql 客户端默认字符集可能是 gbk（服务端/容器内为 utf8mb4），
-- 不声明的话下面这些中文注释与 COMMENT 会报 ERROR 1366 Incorrect string value。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_stackchan_post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `mac` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发帖设备MAC，统一小写冒号格式',
  `contentText` text COLLATE utf8mb4_unicode_ci COMMENT '文本内容',
  `contentImage` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片地址',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stackchan_post_mac` (`mac`),
  KEY `idx_stackchan_post_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan 社区帖子表';

CREATE TABLE IF NOT EXISTS `sys_stackchan_post_comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `postId` bigint unsigned NOT NULL COMMENT '帖子ID',
  `mac` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论设备MAC，统一小写冒号格式',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '评论内容',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stackchan_post_comment_postId` (`postId`),
  KEY `idx_stackchan_post_comment_mac` (`mac`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan 社区帖子评论表';

CREATE TABLE IF NOT EXISTS `sys_stackchan_pano` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '全景图ID',
  `mac` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备MAC，统一小写冒号格式',
  `panoUrl` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '全景图地址',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stackchan_pano_mac` (`mac`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan 设备全景图表';

CREATE TABLE IF NOT EXISTS `sys_stackchan_friend` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
  `macA` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发起方设备MAC，统一小写冒号格式',
  `macB` varchar(17) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '好友设备MAC，统一小写冒号格式',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stackchan_friend_macA_macB` (`macA`,`macB`),
  KEY `idx_stackchan_friend_macB` (`macB`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='StackChan 设备好友关系表';
