-- ============================================================
-- 固件升级（OTA 分批 / 时间窗 / 升级记录）
-- ------------------------------------------------------------
-- 适用：已经用旧版 db/init.sql 建好库的环境。
--      新装环境直接执行 db/init.sql 即可，不需要这份脚本。
-- 执行：mysql -u<user> -p <库名> < db/upgrade/2026-10-28__firmware.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_memory_fact` (
  `factId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '事实ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `memberName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说话人/成员，空表示全家',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事实内容',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类',
  `source` enum('manual','auto') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual' COMMENT '来源',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`factId`),
  KEY `idx_fact_user` (`userId`,`state`),
  KEY `idx_fact_member` (`userId`,`memberName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长期记忆事实表';

CREATE TABLE IF NOT EXISTS `sys_voiceprint` (
  `voiceprintId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '声纹ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `memberName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员称呼',
  `embedding` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '声纹向量 JSON',
  `sampleSeconds` decimal(6,2) DEFAULT '0.00' COMMENT '注册样本时长(秒)',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`voiceprintId`),
  KEY `idx_voiceprint_user` (`userId`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='声纹注册表';

CREATE TABLE IF NOT EXISTS `sys_knowledge_base` (
  `knowledgeId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `roleId` int unsigned DEFAULT NULL COMMENT '绑定角色',
  `embeddingConfigId` int unsigned DEFAULT NULL COMMENT '向量模型配置ID',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createUserId` int unsigned DEFAULT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`knowledgeId`),
  KEY `idx_kb_role` (`roleId`),
  KEY `idx_kb_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS `sys_knowledge_doc` (
  `docId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `knowledgeId` int unsigned NOT NULL COMMENT '知识库ID',
  `fileName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `storedPath` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件存储路径',
  `fileType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `fileSize` bigint unsigned DEFAULT '0' COMMENT '文件大小',
  `chunkCount` int unsigned DEFAULT '0' COMMENT '分片数',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0处理中 1就绪 2失败',
  `errorMsg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `createUserId` int unsigned DEFAULT NULL COMMENT '上传人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`docId`),
  KEY `idx_doc_knowledge` (`knowledgeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS `sys_knowledge_chunk` (
  `chunkId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  `docId` bigint unsigned NOT NULL COMMENT '文档ID',
  `knowledgeId` int unsigned NOT NULL COMMENT '知识库ID',
  `chunkIndex` int unsigned NOT NULL DEFAULT '0' COMMENT '分片序号',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分片正文',
  `embedding` longtext COLLATE utf8mb4_unicode_ci COMMENT '向量 JSON',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`chunkId`),
  KEY `idx_chunk_doc` (`docId`),
  KEY `idx_chunk_knowledge` (`knowledgeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档分片表';

CREATE TABLE IF NOT EXISTS `sys_device_screenshot` (
  `shotId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '截图ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `commandId` bigint unsigned DEFAULT NULL COMMENT '关联指令ID',
  `imageUrl` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回传图片地址',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待回传 1成功 2失败',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因或摘要',
  `createUserId` int unsigned DEFAULT NULL COMMENT '操作人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`shotId`),
  KEY `idx_shot_device` (`deviceId`,`createTime`),
  KEY `idx_shot_command` (`commandId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备截图记录表';

CREATE TABLE IF NOT EXISTS `sys_device_command` (
  `commandId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '指令ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作：reboot/volume/custom',
  `toolName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备 MCP 工具名',
  `arguments` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具入参 JSON',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0已下发 1成功 2失败',
  `result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行结果',
  `createUserId` int unsigned DEFAULT NULL COMMENT '操作人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`commandId`),
  KEY `idx_command_device` (`deviceId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备远程控制指令记录表';

CREATE TABLE IF NOT EXISTS `sys_reminder` (
  `reminderId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '提醒ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标设备ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒标题',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '播报内容',
  `remindTime` datetime NOT NULL COMMENT '下次播报时间',
  `repeatType` enum('once','daily','weekday') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'once' COMMENT '重复方式',
  `state` enum('0','1','2','3') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待播报 1已播报 2已取消 3已错过',
  `lastFireTime` datetime DEFAULT NULL COMMENT '最后播报时间',
  `createUserId` int unsigned DEFAULT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`reminderId`),
  KEY `idx_reminder_due` (`state`,`remindTime`),
  KEY `idx_reminder_device` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音提醒与闹钟表';

CREATE TABLE IF NOT EXISTS `sys_push_message` (
  `pushId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '推送ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推送标题',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '播报内容',
  `targetType` enum('device','user') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'device' COMMENT '目标类型',
  `targetValue` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '逗号分隔的目标ID',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待发送 1已发送 2已取消',
  `totalCount` int unsigned DEFAULT '0' COMMENT '目标设备数',
  `sentCount` int unsigned DEFAULT '0' COMMENT '已下发数',
  `offlineCount` int unsigned DEFAULT '0' COMMENT '离线跳过数',
  `sentTime` datetime DEFAULT NULL COMMENT '发送时间',
  `createUserId` int unsigned DEFAULT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`pushId`),
  KEY `idx_push_state` (`state`,`sentTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主动推送消息表';

CREATE TABLE IF NOT EXISTS `sys_device_channel` (
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `channel` enum('websocket','mqtt') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'websocket' COMMENT '最近一次使用的接入通道',
  `clientId` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MQTT 客户端ID',
  `onlineState` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '通道是否在线：1在线 0离线',
  `lastSeenTime` datetime DEFAULT NULL COMMENT '最后一次心跳或上行消息时间',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`deviceId`),
  KEY `idx_channel_online` (`onlineState`,`lastSeenTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备接入通道与在线状态表';

CREATE TABLE IF NOT EXISTS `sys_firmware` (
  `firmwareId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '固件ID',
  `version` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '固件版本号，如 1.2.3',
  `chipModel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用芯片型号，空表示不限',
  `fileUrl` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '固件文件地址',
  `fileSize` bigint unsigned DEFAULT '0' COMMENT '文件大小(字节)',
  `md5` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件MD5，供设备校验',
  `releaseNotes` text COLLATE utf8mb4_unicode_ci COMMENT '发布说明',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态(1启用 0停用)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`firmwareId`),
  UNIQUE KEY `uk_firmware_version` (`version`,`chipModel`),
  KEY `idx_firmware_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备固件版本表';

CREATE TABLE IF NOT EXISTS `sys_firmware_release` (
  `releaseId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '升级任务ID',
  `firmwareId` int unsigned NOT NULL COMMENT '目标固件ID',
  `releaseName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `strategy` enum('all','percent','whitelist') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'all' COMMENT '投放策略：all-全量, percent-按比例, whitelist-白名单',
  `percent` tinyint unsigned DEFAULT '0' COMMENT '灰度百分比(1-100)，strategy=percent 时有效',
  `whitelist` text COLLATE utf8mb4_unicode_ci COMMENT '白名单设备ID，逗号分隔，strategy=whitelist 时有效',
  `startTime` datetime DEFAULT NULL COMMENT '时间窗开始，空表示立即生效',
  `endTime` datetime DEFAULT NULL COMMENT '时间窗结束，空表示不设结束',
  `state` enum('0','1','2','3') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待开始 1进行中 2已暂停 3已结束',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`releaseId`),
  KEY `idx_release_firmware` (`firmwareId`),
  KEY `idx_release_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='固件升级任务表';

CREATE TABLE IF NOT EXISTS `sys_firmware_upgrade_log` (
  `logId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `releaseId` int unsigned DEFAULT NULL COMMENT '升级任务ID',
  `firmwareId` int unsigned DEFAULT NULL COMMENT '固件ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `chipModel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '芯片型号',
  `fromVersion` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '升级前版本',
  `toVersion` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '升级目标版本',
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备IP',
  `status` enum('0','1','2') COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '0已下发 1成功 2失败',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`logId`),
  KEY `idx_upgrade_device` (`deviceId`,`createTime`),
  KEY `idx_upgrade_release` (`releaseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='固件升级记录表';

-- ---------------- 菜单与接口权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '固件升级', 'system:firmware', 'menu', '/firmware', 'page/Firmware', 'cloud-upload', 4, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:firmware');

SET @firmwareMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:firmware' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@firmwareMenuId, '固件列表接口', 'system:firmware:api:list', 'api', 1, '0', '1'),
  (@firmwareMenuId, '固件创建接口', 'system:firmware:api:create', 'api', 2, '0', '1'),
  (@firmwareMenuId, '固件更新接口', 'system:firmware:api:update', 'api', 3, '0', '1'),
  (@firmwareMenuId, '固件删除接口', 'system:firmware:api:delete', 'api', 4, '0', '1'),
  (@firmwareMenuId, '升级任务列表接口', 'system:firmware:release:api:list', 'api', 5, '0', '1'),
  (@firmwareMenuId, '升级任务创建接口', 'system:firmware:release:api:create', 'api', 6, '0', '1'),
  (@firmwareMenuId, '升级任务更新接口', 'system:firmware:release:api:update', 'api', 7, '0', '1'),
  (@firmwareMenuId, '升级任务删除接口', 'system:firmware:release:api:delete', 'api', 8, '0', '1'),
  (@firmwareMenuId, '升级任务状态接口', 'system:firmware:release:api:state', 'api', 9, '0', '1'),
  (@firmwareMenuId, '升级记录列表接口', 'system:firmware:upgrade-log:api:list', 'api', 10, '0', '1');

-- ---------------- 设备接入自检接口权限 ----------------

SET @deviceMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:device' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES (@deviceMenuId, '设备自检接口', 'system:device:api:self-check', 'api', 7, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:device:api:self-check';

-- ---------------- 主动推送权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '主动推送', 'system:push', 'menu', '/push', 'page/Push', 'notification', 5, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:push');

SET @pushMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:push' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@pushMenuId, '推送列表接口', 'system:push:api:list', 'api', 1, '0', '1'),
  (@pushMenuId, '推送创建接口', 'system:push:api:create', 'api', 2, '0', '1'),
  (@pushMenuId, '推送发送接口', 'system:push:api:send', 'api', 3, '0', '1'),
  (@pushMenuId, '推送删除接口', 'system:push:api:delete', 'api', 4, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:push%';

-- ---------------- 语音提醒权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '语音提醒', 'system:reminder', 'menu', '/reminder', 'page/Reminder', 'clock', 6, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:reminder');

SET @reminderMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:reminder' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@reminderMenuId, '提醒列表接口', 'system:reminder:api:list', 'api', 1, '0', '1'),
  (@reminderMenuId, '提醒创建接口', 'system:reminder:api:create', 'api', 2, '0', '1'),
  (@reminderMenuId, '提醒更新接口', 'system:reminder:api:update', 'api', 3, '0', '1'),
  (@reminderMenuId, '提醒取消接口', 'system:reminder:api:cancel', 'api', 4, '0', '1'),
  (@reminderMenuId, '提醒删除接口', 'system:reminder:api:delete', 'api', 5, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:reminder%';

-- ---------------- 设备远程控制权限（挂在设备菜单下）----------------

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@deviceMenuId, '设备远程控制接口', 'system:device:api:command', 'api', 8, '0', '1'),
  (@deviceMenuId, '设备指令记录接口', 'system:device:command:api:list', 'api', 9, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission`
WHERE `permissionKey` IN ('system:device:api:command', 'system:device:command:api:list');

-- ---------------- 设备截图权限 ----------------

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@deviceMenuId, '设备截图接口', 'system:device:api:screenshot', 'api', 10, '0', '1'),
  (@deviceMenuId, '设备截图记录接口', 'system:device:screenshot:api:list', 'api', 11, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission`
WHERE `permissionKey` IN ('system:device:api:screenshot', 'system:device:screenshot:api:list');

-- ---------------- 知识库权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '知识库', 'system:knowledge', 'menu', '/knowledge', 'page/Knowledge', 'book', 7, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:knowledge');

SET @knowledgeMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:knowledge' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@knowledgeMenuId, '知识库列表接口', 'system:knowledge:api:list', 'api', 1, '0', '1'),
  (@knowledgeMenuId, '知识库创建接口', 'system:knowledge:api:create', 'api', 2, '0', '1'),
  (@knowledgeMenuId, '知识库更新接口', 'system:knowledge:api:update', 'api', 3, '0', '1'),
  (@knowledgeMenuId, '知识库删除接口', 'system:knowledge:api:delete', 'api', 4, '0', '1'),
  (@knowledgeMenuId, '文档上传接口', 'system:knowledge:doc:api:upload', 'api', 5, '0', '1'),
  (@knowledgeMenuId, '文档删除接口', 'system:knowledge:doc:api:delete', 'api', 6, '0', '1'),
  (@knowledgeMenuId, '检索测试接口', 'system:knowledge:api:search', 'api', 7, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:knowledge%';

-- ---------------- 声纹识别权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '声纹识别', 'system:voiceprint', 'menu', '/voiceprint', 'page/Voiceprint', 'sound', 8, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:voiceprint');

SET @voiceprintMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:voiceprint' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@voiceprintMenuId, '声纹列表接口', 'system:voiceprint:api:list', 'api', 1, '0', '1'),
  (@voiceprintMenuId, '声纹注册接口', 'system:voiceprint:api:create', 'api', 2, '0', '1'),
  (@voiceprintMenuId, '声纹删除接口', 'system:voiceprint:api:delete', 'api', 3, '0', '1'),
  (@voiceprintMenuId, '声纹识别测试接口', 'system:voiceprint:api:match', 'api', 4, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:voiceprint%';

-- ---------------- 监控面板接口权限（挂在 Dashboard 菜单下）----------------

SET @dashboardMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:dashboard' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@dashboardMenuId, '监控概览接口', 'system:stats:api:overview', 'api', 1, '0', '1'),
  (@dashboardMenuId, '消息趋势接口', 'system:stats:api:messages', 'api', 2, '0', '1'),
  (@dashboardMenuId, 'Top设备接口', 'system:stats:api:devices', 'api', 3, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:stats%';

-- ---------------- 长期记忆权限 ----------------

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT NULL, '长期记忆', 'system:memory-fact', 'menu', '/memory-fact', 'page/MemoryFact', 'database', 9, '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM (SELECT `permissionKey` FROM `sys_permission`) t WHERE t.`permissionKey` = 'system:memory-fact');

SET @memoryFactMenuId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey` = 'system:memory-fact' LIMIT 1);

INSERT IGNORE INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
VALUES
  (@memoryFactMenuId, '记忆列表接口', 'system:memory-fact:api:list', 'api', 1, '0', '1'),
  (@memoryFactMenuId, '记忆创建接口', 'system:memory-fact:api:create', 'api', 2, '0', '1'),
  (@memoryFactMenuId, '记忆删除接口', 'system:memory-fact:api:delete', 'api', 3, '0', '1');

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:memory-fact%';

-- 管理员角色（authRoleId=1）默认拿到全部固件权限；普通角色请到「权限角色」页面按需勾选
INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, `permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:firmware%';
