-- 曲库管理台 v2：表 + 全部曲库接口权限 + 管理员授权（一次执行永久兜底）
-- ------------------------------------------------------------
-- 幂等：可重复执行。所有权限按 permissionKey 判断、授权按 permissionKey 关联，
-- 不写死 permissionId；重复执行不会插重复行，也不会把权限授给错误 id。
--
-- 这份脚本覆盖三件事：
--   1. sys_music / sys_music_play_log 建表（旧版 music_library 脚本可能没跑过）
--   2. system:file:api 下的 list/delete/plays/update/sync/preview 权限行
--   3. 管理员角色（authRoleId=1）默认拿到全部 system:file* 权限
--      —— 这是「进曲库页就权限不足」的根因修复
--
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-10-30__music_console.sql
--       然后清掉 Redis 里的权限缓存：XiaoZhi:Permission*
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

SET @musicFileMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:file' LIMIT 1);

-- 列表
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲库列表', 'system:file:api:list', 'api', 3, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:list');

-- 删除
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '删除曲库文件', 'system:file:api:delete', 'api', 4, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:delete');

-- 点播记录
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '点播记录', 'system:file:api:plays', 'api', 5, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:plays');

-- 编辑 / 上下架
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲目更新接口', 'system:file:api:update', 'api', 6, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:update');

-- 目录同步
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲库同步接口', 'system:file:api:sync', 'api', 7, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:sync');

-- 试听
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲目试听接口', 'system:file:api:preview', 'api', 8, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:preview');

-- 管理员角色默认拿到全部文件能力权限（含上传/曲库各接口）
INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:file%';
