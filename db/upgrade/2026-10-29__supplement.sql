-- 2026-10-29 补齐遗漏的增量：记忆图谱 / 音色克隆 / 对话指标 / 设备分组
-- 这些表与权限在 10-28 的升级脚本里没有覆盖到（只写进了 init.sql），已建库需要单独补。

CREATE TABLE IF NOT EXISTS `sys_memory_entity` (
  `entityId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '实体ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实体名，如 妈妈/乌龙茶',
  `type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型：person/thing/place/other',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`entityId`),
  UNIQUE KEY `uk_entity_user_name` (`userId`,`name`),
  KEY `idx_entity_user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记忆图谱实体表';

CREATE TABLE IF NOT EXISTS `sys_memory_relation` (
  `relationId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `fromEntityId` bigint unsigned NOT NULL COMMENT '起点实体',
  `toEntityId` bigint unsigned NOT NULL COMMENT '终点实体',
  `relation` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系名，如 喜欢/住在/女儿',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`relationId`),
  KEY `idx_relation_user` (`userId`),
  KEY `idx_relation_from` (`fromEntityId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记忆图谱关系表';

CREATE TABLE IF NOT EXISTS `sys_dialogue_metric` (
  `metricId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备ID',
  `roleId` int unsigned DEFAULT NULL COMMENT '角色ID',
  `sessionId` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `promptTokens` int unsigned DEFAULT '0' COMMENT '提示词 token',
  `completionTokens` int unsigned DEFAULT '0' COMMENT '回复 token',
  `totalTokens` int unsigned DEFAULT '0' COMMENT '合计 token',
  `latencyMs` bigint unsigned DEFAULT '0' COMMENT '本轮从收句到首句可播的耗时(毫秒)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`metricId`),
  KEY `idx_metric_time` (`createTime`),
  KEY `idx_metric_device` (`deviceId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话轮次指标表';

CREATE TABLE IF NOT EXISTS `sys_voice_clone` (
  `cloneId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '克隆ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `voiceName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '音色名称',
  `provider` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务商：aliyun/volcengine',
  `voiceId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '服务商返回的音色ID',
  `samplePath` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样本音频存储路径',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0处理中 1就绪 2失败',
  `errorMsg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cloneId`),
  KEY `idx_clone_user` (`userId`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音色克隆表';

CREATE TABLE IF NOT EXISTS `sys_device_group` (
  `groupId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `groupName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名，如 客厅',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`groupId`),
  KEY `idx_group_user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组表';

CREATE TABLE IF NOT EXISTS `sys_device_group_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `groupId` bigint unsigned NOT NULL COMMENT '分组ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_device` (`groupId`,`deviceId`),
  KEY `idx_member_device` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组关系表';

INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (100,NULL,'声纹识别','system:voiceprint','menu','/voiceprint','page/Voiceprint','sound',8,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (101,100,'声纹列表接口','system:voiceprint:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (102,100,'声纹注册接口','system:voiceprint:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (103,100,'声纹删除接口','system:voiceprint:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (104,100,'声纹识别测试接口','system:voiceprint:api:match','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (105,1,'监控概览接口','system:stats:api:overview','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (106,1,'消息趋势接口','system:stats:api:messages','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (107,1,'Top设备接口','system:stats:api:devices','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (108,NULL,'长期记忆','system:memory-fact','menu','/memory-fact','page/MemoryFact','database',9,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (109,108,'记忆列表接口','system:memory-fact:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (110,108,'记忆创建接口','system:memory-fact:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (111,108,'记忆删除接口','system:memory-fact:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (112,NULL,'记忆图谱','system:memory-graph','menu','/memory-graph','page/MemoryGraph','share-alt',10,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (113,112,'图谱查询接口','system:memory-graph:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (114,112,'关系创建接口','system:memory-graph:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (115,NULL,'音色克隆','system:voice-clone','menu','/voice-clone','page/VoiceClone','audio',11,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (116,115,'克隆列表接口','system:voice-clone:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (117,115,'克隆创建接口','system:voice-clone:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT IGNORE INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (118,115,'克隆删除接口','system:voice-clone:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');

INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (128,1,100,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (129,1,101,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (130,1,102,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (131,1,103,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (132,1,104,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (133,1,105,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (134,1,106,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (135,1,107,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (136,1,108,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (137,1,109,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (138,1,110,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (139,1,111,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (140,1,112,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (141,1,113,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (142,1,114,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (143,1,115,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (144,1,116,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (145,1,117,'2026-10-28 00:37:45');
INSERT IGNORE INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (146,1,118,'2026-10-28 00:37:45');
