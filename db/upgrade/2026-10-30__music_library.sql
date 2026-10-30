-- 曲库与点播记录（方案 C）
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

INSERT IGNORE INTO `sys_permission` (`permissionId`,`parentId`,`name`,`permissionKey`,`permissionType`,`path`,`component`,`icon`,`sort`,`visible`,`status`,`createTime`,`updateTime`)
VALUES (212,17,'点播记录','system:file:api:plays','api',NULL,NULL,NULL,5,'1','1',NOW(),NOW());
INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`,`permissionId`,`createTime`) VALUES (1,212,NOW());
