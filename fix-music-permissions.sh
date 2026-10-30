#!/usr/bin/env bash
# 曲库权限一键修复（在服务器上执行，幂等，可重复跑）
#
# 用法：
#   cd /opt/xiaozhi
#   bash fix-music-permissions.sh
#
# 解决什么问题：
#   旧版升级脚本只建了 system:file:api:list / plays / delete 三条权限行，
#   却没给管理员角色（authRoleId=1）授权，于是管理员打开「智能体 → 曲库」
#   接口恒定 403，页面弹「权限不足」。本脚本按 permissionKey 幂等补齐：
#     1. sys_music / sys_music_play_log 建表（旧库可能没建）
#     2. 补 list / delete / plays / update / sync / preview 六条权限行
#     3. 把全部 system:file* 权限授给 authRoleId=1
#     4. 清 Redis 里的 XiaoZhi:Permission* 缓存，让 Sa-Token 重新加载权限
set -euo pipefail

cd /opt/xiaozhi 2>/dev/null || { echo "找不到 /opt/xiaozhi，请在 compose 所在目录执行"; exit 1; }

echo "=== [1/3] 执行幂等 SQL ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' <<'SQL'
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

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲库列表', 'system:file:api:list', 'api', 3, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:list');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '删除曲库文件', 'system:file:api:delete', 'api', 4, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:delete');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '点播记录', 'system:file:api:plays', 'api', 5, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:plays');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲目更新接口', 'system:file:api:update', 'api', 6, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:update');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲库同步接口', 'system:file:api:sync', 'api', 7, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:sync');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT @musicFileMenuId, '曲目试听接口', 'system:file:api:preview', 'api', 8, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:preview');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:file%';
SQL

echo "=== [2/3] 清理 Redis 权限缓存 ==="
if docker compose ps --format '{{.Service}}' 2>/dev/null | grep -qx 'redis'; then
  docker compose exec -T redis sh -c "redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do redis-cli del \"\$k\" >/dev/null; done" || true
  echo "权限缓存已清理"
else
  echo "redis 容器未运行，跳过（重启 server 后权限也会重新加载）"
fi

echo "=== [3/3] 结果核对 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT permissionId, permissionKey, status FROM sys_permission ORDER BY permissionId DESC LIMIT 12; SELECT COUNT(*) AS adminPermissionCount FROM sys_auth_role_permission WHERE authRoleId = 1;"'

echo
echo "完成：退出后台重新登录（权限树会重新下发），再打开「智能体 → 曲库」"
