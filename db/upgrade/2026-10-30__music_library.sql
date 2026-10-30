-- 曲库与点播记录（方案 C）
-- ------------------------------------------------------------
-- 幂等：可重复执行。权限行按 permissionKey 判断，授权行只认 authRoleId=1，
-- 不依赖固定的 permissionId —— 现网可能已经部分执行过旧版本，写死 id 会把权限授错对象。
-- 适用：已经用旧版 db/init.sql 建好库的环境；新装环境直接执行 db/init.sql 即可。
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-10-30__music_library.sql
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `sys_music` (
  `musicId` bigint unsigned NOT NULL AUTO_INCREMENT,
  `fileName` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `artist` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aliases` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `durationSec` int DEFAULT NULL,
  `sizeBytes` bigint DEFAULT NULL,
  `playCount` int DEFAULT '0',
  `state` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`musicId`),
  UNIQUE KEY `uk_music_file` (`fileName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='曲库';

CREATE TABLE IF NOT EXISTS `sys_music_play_log` (
  `logId` bigint unsigned NOT NULL AUTO_INCREMENT,
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `musicId` bigint unsigned DEFAULT NULL,
  `keyword` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `songName` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finished` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`logId`),
  KEY `idx_play_music` (`musicId`),
  KEY `idx_play_device` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='曲库点播记录';

-- ---------------- 点播记录接口权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:file' LIMIT 1),
       '点播记录', 'system:file:api:plays', 'api', 5, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:plays');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:file:api:plays';
