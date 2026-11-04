-- 2026-11-03 操作日志查询：后台补齐「操作日志」菜单/接口权限，并授权给管理员角色。
-- 背景：sys_operation_log 之前只有写入与 180 天清理，没有查询接口与页面，审计数据只能查库。
-- 约定：幂等（INSERT ... WHERE NOT EXISTS / INSERT IGNORE，可重复执行）；
--       只按 permissionKey 关联，不写死 permissionId / 授权行 id —— 存量库的主键水位不可预知，
--       写死 id 会把权限授错对象或静默跳过（曲库上线时踩过同样的坑）。
--       只加权限与授权行，不动业务表，不删任何已有数据。
-- 存量库执行后清一次 Redis 权限缓存（XiaoZhi:Permission*），或让管理员在后台重存一次角色权限，
-- 否则权限缓存最长 7 天内仍是旧集合，打开页面会 403。
-- 新装环境直接执行 db/init.sql 即可，无需本脚本。

-- Windows 本机的 mysql 客户端默认字符集可能是 gbk（服务器/容器内为 utf8mb4），
-- 不声明的话下面这些中文权限名会报 ERROR 1366 Incorrect string value。
SET NAMES utf8mb4;

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '操作日志', 'system:operation-log', 'menu', '/operation-log', 'page/OperationLog', 'file-search', 13, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:operation-log');

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:operation-log' LIMIT 1),
       '操作日志列表接口', 'system:operation-log:api:list', 'api', 1, '0', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:operation-log:api:list');

-- 管理员角色默认拿到操作日志权限（表上有 (authRoleId, permissionId) 唯一索引，IGNORE 可重复执行）
INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission`
WHERE `permissionKey` IN ('system:operation-log', 'system:operation-log:api:list');
