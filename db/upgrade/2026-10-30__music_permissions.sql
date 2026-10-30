-- 曲库管理页：列表 / 删除接口权限
-- ------------------------------------------------------------
-- 幂等：可重复执行，按 permissionKey 判断，不依赖固定 permissionId。
-- 现网「进曲库页就权限不足」的直接原因就是：旧脚本只建了权限行，
-- 没有把权限授给管理员角色（authRoleId=1），这里一并补齐。
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-10-30__music_permissions.sql
-- ------------------------------------------------------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:file' LIMIT 1),
       '曲库列表', 'system:file:api:list', 'api', 3, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:list');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:file' LIMIT 1),
       '删除曲库文件', 'system:file:api:delete', 'api', 4, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:file:api:delete');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission`
WHERE `permissionKey` IN ('system:file:api:list', 'system:file:api:delete');
