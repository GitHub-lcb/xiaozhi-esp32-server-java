-- =============================================================================
-- xiaozhi-esp32-server-java · 数据库一键初始化脚本
--
-- 一条命令完成全部初始化：建账号 → 建库 → 建表 → 灌基础数据。
-- 这是项目唯一的数据库结构与初始数据来源，应用不再执行任何迁移。
--
-- 用法（新项目只需这一条）：
--   mysql -uroot -p < db/init.sql
--
-- docker compose -f docker-compose-db.yml up -d 时，本文件会被挂载到容器的
-- /docker-entrypoint-initdb.d/ 自动执行，无需手动导入。
--
-- 默认账号：xiaozhi / 123456（库名 xiaozhi），应用侧见 application-dev.yml。
-- 管理后台默认登录：admin / 123456，首次登录后请立即修改。
--
-- 注意：本脚本含 DROP TABLE，只用于全新库，不要在有数据的库上重复执行。
-- 维护方式：结构变更直接改本文件（改表结构 + 同步实体），没有增量迁移脚本。
--           自动化测试 InitSqlGuardTest 会在空库上真跑本文件，并核对表结构装得下每个 DO 的字段。
-- =============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------- 账号与库 ----
-- >>> bootstrap-only 建账号、授权、建库需要全局权限且写死库名，只给人工执行；自动化测试跳过本区块
CREATE USER IF NOT EXISTS 'xiaozhi'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
CREATE USER IF NOT EXISTS 'xiaozhi'@'%'         IDENTIFIED WITH mysql_native_password BY '123456';
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'%';
FLUSH PRIVILEGES;

CREATE DATABASE IF NOT EXISTS `xiaozhi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xiaozhi`;
-- <<< bootstrap-only

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `sys_auth_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auth_role` (
  `authRoleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `authRoleName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `roleKey` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色标识',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态(1正常 0禁用)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`authRoleId`),
  UNIQUE KEY `uk_role_key` (`roleKey`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_auth_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auth_role_permission` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `authRoleId` int unsigned NOT NULL COMMENT '角色ID',
  `permissionId` int unsigned NOT NULL COMMENT '权限ID',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_role_permission` (`authRoleId`,`permissionId`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_code` (
  `codeId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '验证码',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `account` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收码账号：邮箱或手机号，设备码为NULL',
  `deviceId` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备ID',
  `sessionId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sessionID',
  `audioPath` text COLLATE utf8mb4_unicode_ci COMMENT '语音文件路径',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`codeId`),
  KEY `account` (`account`),
  KEY `deviceId` (`deviceId`),
  KEY `code_createTime` (`code`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `configId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID，主键',
  `userId` int unsigned NOT NULL COMMENT '创建用户ID',
  `configType` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置类型(llm, stt, tts等)',
  `modelType` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'LLM模型类型(chat, vision, intent, embedding等)',
  `provider` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务提供商(openai, vosk, aliyun, tencent等)',
  `configName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置名称',
  `configDesc` text COLLATE utf8mb4_unicode_ci COMMENT '配置描述',
  `appId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'APP ID',
  `apiKey` text COLLATE utf8mb4_unicode_ci COMMENT 'API密钥',
  `apiSecret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥',
  `ak` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Access Key',
  `sk` text COLLATE utf8mb4_unicode_ci COMMENT 'Secret Key',
  `apiUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API地址',
  `isDefault` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否为默认配置: 1-是, 0-否',
  `enableThinking` tinyint(1) DEFAULT NULL COMMENT '是否启用思考模式',
  `contextLength` int unsigned DEFAULT NULL COMMENT '模型上下文长度(token)',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `defaultKey` varchar(80) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if(((`isDefault` = _utf8mb4'1') and (`state` = _utf8mb4'1')),concat(`configType`,_utf8mb4':',if((`configType` = _utf8mb4'llm'),ifnull(`modelType`,_utf8mb4''),_utf8mb4'')),NULL)) VIRTUAL COMMENT '唯一默认约束键，非默认行为 NULL',
  PRIMARY KEY (`configId`),
  UNIQUE KEY `uk_config_default` (`defaultKey`),
  KEY `userId` (`userId`),
  KEY `configType` (`configType`),
  KEY `provider` (`provider`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表(模型、语音识别、语音合成等)';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_conversation` (
  `sessionId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户ID',
  `roleId` int unsigned NOT NULL COMMENT '角色ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后对话时间',
  PRIMARY KEY (`sessionId`),
  KEY `idx_conversation_user_update` (`userId`,`updateTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Web 聊天会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device` (
  `deviceId` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID，主键',
  `deviceName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称',
  `mcpList` text COLLATE utf8mb4_unicode_ci COMMENT '设备 MCP 工具列表，逗号分隔的工具名称',
  `roleId` int unsigned DEFAULT NULL COMMENT '角色ID，主键',
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地理位置',
  `wifiName` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'WiFi名称',
  `chipModelName` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '芯片型号',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '固件版本',
  `state` enum('0','1','2') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '设备状态：1-在线，0-离线，2-待机',
  `userId` int unsigned NOT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`deviceId`),
  KEY `deviceName` (`deviceName`),
  KEY `userId` (`userId`),
  KEY `roleId` (`roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device_group_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `groupId` bigint unsigned NOT NULL COMMENT '分组ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_device` (`groupId`,`deviceId`),
  KEY `idx_member_device` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device_group` (
  `groupId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `groupName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名，如 客厅',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`groupId`),
  KEY `idx_group_user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dialogue_metric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dialogue_metric` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_voice_clone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_voice_clone` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_memory_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_memory_relation` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_memory_entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_memory_entity` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_memory_fact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_memory_fact` (
  `factId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '事实ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `memberName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说话人/成员，空表示对全家人有效',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事实内容',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类：偏好/日程/关系/其它',
  `source` enum('manual','auto') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual-人工录入 auto-自动抽取',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`factId`),
  KEY `idx_fact_user` (`userId`,`state`),
  KEY `idx_fact_member` (`userId`,`memberName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长期记忆事实表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_voiceprint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_voiceprint` (
  `voiceprintId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '声纹ID',
  `userId` int unsigned NOT NULL COMMENT '所属用户',
  `memberName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员称呼，如 爸爸/妈妈',
  `embedding` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '声纹向量 JSON',
  `sampleSeconds` decimal(6,2) DEFAULT '0.00' COMMENT '注册样本时长(秒)',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`voiceprintId`),
  KEY `idx_voiceprint_user` (`userId`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='声纹注册表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_knowledge_chunk` (
  `chunkId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  `docId` bigint unsigned NOT NULL COMMENT '文档ID',
  `knowledgeId` int unsigned NOT NULL COMMENT '知识库ID',
  `chunkIndex` int unsigned NOT NULL DEFAULT '0' COMMENT '分片序号',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分片正文',
  `embedding` longtext COLLATE utf8mb4_unicode_ci COMMENT '向量，JSON 数组；为空表示未向量化',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`chunkId`),
  KEY `idx_chunk_doc` (`docId`),
  KEY `idx_chunk_knowledge` (`knowledgeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档分片表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_knowledge_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_knowledge_doc` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_knowledge_base` (
  `knowledgeId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `roleId` int unsigned DEFAULT NULL COMMENT '绑定角色，空表示不自动挂载',
  `embeddingConfigId` int unsigned DEFAULT NULL COMMENT '向量模型配置ID，空表示用全局默认',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `createUserId` int unsigned DEFAULT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`knowledgeId`),
  KEY `idx_kb_role` (`roleId`),
  KEY `idx_kb_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device_screenshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device_screenshot` (
  `shotId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '截图ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `commandId` bigint unsigned DEFAULT NULL COMMENT '关联的指令ID',
  `imageUrl` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回传图片地址',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待回传 1成功 2失败',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因或设备返回摘要',
  `createUserId` int unsigned DEFAULT NULL COMMENT '操作人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`shotId`),
  KEY `idx_shot_device` (`deviceId`,`createTime`),
  KEY `idx_shot_command` (`commandId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备截图记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device_command`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device_command` (
  `commandId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '指令ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作：reboot/volume/custom',
  `toolName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实际调用的设备 MCP 工具名',
  `arguments` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具入参 JSON',
  `state` enum('0','1','2') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0已下发 1成功 2失败',
  `result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行结果或失败原因',
  `createUserId` int unsigned DEFAULT NULL COMMENT '操作人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`commandId`),
  KEY `idx_command_device` (`deviceId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备远程控制指令记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_reminder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_reminder` (
  `reminderId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '提醒ID',
  `deviceId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标设备ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒标题',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '播报内容',
  `remindTime` datetime NOT NULL COMMENT '下次播报时间',
  `repeatType` enum('once','daily','weekday') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'once' COMMENT '重复方式：once-一次性 daily-每天 weekday-工作日',
  `state` enum('0','1','2','3') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0待播报 1已播报 2已取消 3已错过',
  `lastFireTime` datetime DEFAULT NULL COMMENT '最后一次播报时间',
  `createUserId` int unsigned DEFAULT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`reminderId`),
  KEY `idx_reminder_due` (`state`,`remindTime`),
  KEY `idx_reminder_device` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音提醒与闹钟表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_push_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_push_message` (
  `pushId` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '推送ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推送标题',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '播报内容',
  `targetType` enum('device','user') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'device' COMMENT '目标类型：device-按设备 user-按用户',
  `targetValue` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标值，逗号分隔的设备ID或用户ID',
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_device_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_device_channel` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_firmware`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_firmware` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_firmware_release`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_firmware_release` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_firmware_upgrade_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_firmware_upgrade_log` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_mcp_tool_exclude`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_mcp_tool_exclude` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `excludeType` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '过滤类型：global-全局过滤，role-角色过滤',
  `bindType` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绑定类型：mcp_server-MCP服务器，mcp_endpoint-MCP接入点',
  `bindCode` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绑定的MCP服务器代码或接入点标识',
  `bindKey` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绑定键：roleId，全局过滤时为0',
  `excludeTools` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '要排除的工具函数名称列表，JSON数组格式',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`excludeType`,`bindType`,`bindCode`,`bindKey`),
  KEY `idx_bind_key` (`bindKey`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具过滤配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `messageId` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键，自增',
  `userId` int unsigned DEFAULT NULL COMMENT '用户ID',
  `deviceId` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `sessionId` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `source` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'device' COMMENT '消息来源: web|device|...（存量默认 device）',
  `sender` enum('user','assistant','tool') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息发送方：user-用户，assistant-人工智能，tool-工具响应',
  `roleId` int unsigned DEFAULT NULL COMMENT 'AI扮演的角色ID',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `metadata` json DEFAULT NULL COMMENT 'UserMessage 附加元数据(speaker/emotion 等)，JSON 格式',
  `messageType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息类型',
  `audioPath` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '音频文件路径（本地相对路径或云存储 URL）',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态：1-有效，0-删除',
  `toolCalls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具调用详情JSON，包含name/arguments/result',
  `createTime` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '消息发送时间(毫秒精度)',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`messageId`),
  KEY `userId` (`userId`),
  KEY `deviceId` (`deviceId`),
  KEY `sessionId` (`sessionId`),
  KEY `createTime` (`createTime`),
  KEY `deviceId_roleId_createTime` (`deviceId`,`roleId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人与AI对话消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` int DEFAULT NULL COMMENT '操作用户ID',
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `module` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作模块',
  `operation` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作描述',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP方法',
  `url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `handler` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Controller#method',
  `params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数(脱敏后)',
  `success` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否成功: 1成功 0失败',
  `errorMsg` text COLLATE utf8mb4_unicode_ci COMMENT '失败时的错误信息',
  `costMs` int DEFAULT NULL COMMENT '接口耗时(毫秒)',
  `createTime` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`),
  KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `permissionId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `parentId` int unsigned DEFAULT NULL COMMENT '父权限ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `permissionKey` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
  `permissionType` enum('menu','button','api') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限类型：菜单、按钮、接口',
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端组件路径',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  `visible` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否可见(1可见 0隐藏)',
  `status` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态(1正常 0禁用)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permissionId`),
  UNIQUE KEY `uk_permission_key` (`permissionKey`)
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `roleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
  `roleName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `roleDesc` text COLLATE utf8mb4_unicode_ci COMMENT '角色描述',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像（本地相对路径或云存储 URL）',
  `ttsId` int DEFAULT NULL COMMENT 'TTS服务ID',
  `modelId` int unsigned DEFAULT NULL COMMENT '模型ID',
  `sttId` int unsigned DEFAULT NULL COMMENT 'STT服务ID',
  `sttHotwords` text COLLATE utf8mb4_unicode_ci COMMENT '语音识别热词，每行一个「词 [权重]」',
  `vadSpeechTh` float DEFAULT '0.5' COMMENT '语音检测阈值',
  `vadSilenceTh` float DEFAULT '0.3' COMMENT '静音检测阈值',
  `vadEnergyTh` float DEFAULT '0.01' COMMENT '能量检测阈值',
  `vadSilenceMs` int DEFAULT '800' COMMENT '静音检测时间',
  `inactiveTimeoutSeconds` int NOT NULL DEFAULT '60' COMMENT '会话空闲自动结束秒数，0表示关闭',
  `voiceName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色语音名称',
  `ttsPitch` double DEFAULT '1' COMMENT '语音音调',
  `ttsSpeed` double DEFAULT '1' COMMENT '语音语速',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数',
  `topP` double DEFAULT '1' COMMENT 'Top-P参数',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `isDefault` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否默认角色：1-是，0-否',
  `userId` int unsigned NOT NULL COMMENT '创建人',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `defaultKey` int unsigned GENERATED ALWAYS AS (if((`isDefault` = _utf8mb4'1'),`userId`,NULL)) VIRTUAL COMMENT '唯一默认约束键，非默认行为 NULL',
  PRIMARY KEY (`roleId`),
  UNIQUE KEY `uk_role_default` (`defaultKey`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_summary` (
  `deviceId` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID，联合索引第一个字段',
  `roleId` int unsigned NOT NULL COMMENT '角色ID，联合索引第二个字段',
  `sessionId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Web 会话ID，设备端为空',
  `lastMessageTimestamp` datetime(3) NOT NULL COMMENT '最后一条消息的创建时间戳，精确到毫秒，联合索引第三个字段',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '摘要内容。提炼出来的重要信息',
  `promptTokens` int unsigned DEFAULT '0' COMMENT '摘要动作本身消耗的promptTokens',
  `completionTokens` int unsigned DEFAULT '0' COMMENT '摘要动作本身消耗的completionTokens',
  `createTime` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建日期，进行摘要的实际时间戳，精确到毫秒',
  PRIMARY KEY (`deviceId`,`roleId`,`lastMessageTimestamp` DESC),
  KEY `idx_summary_session` (`sessionId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息摘要记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_template` (
  `userId` int unsigned NOT NULL COMMENT '创建用户ID',
  `templateId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `templateName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `templateDesc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板描述',
  `templateContent` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板内容',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板分类',
  `isDefault` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否为默认配置: 1-是, 0-否',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态(1启用 0禁用)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `defaultKey` int unsigned GENERATED ALWAYS AS (if(((`isDefault` = _utf8mb4'1') and (`state` = _utf8mb4'1')),`userId`,NULL)) VIRTUAL COMMENT '唯一默认约束键，非默认行为 NULL',
  PRIMARY KEY (`templateId`),
  UNIQUE KEY `uk_template_default` (`defaultKey`),
  KEY `category` (`category`),
  KEY `templateName` (`templateName`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `userId` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tel` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `authRoleId` int unsigned NOT NULL DEFAULT '2' COMMENT '角色ID',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像（本地相对路径或云存储 URL）',
  `state` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '1-正常 0-禁用',
  `loginIp` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isAdmin` enum('0','1') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loginTime` datetime DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_tel` (`tel`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_auth` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` int unsigned NOT NULL COMMENT '用户ID',
  `openId` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '第三方平台用户标识',
  `unionId` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方平台开放平台标识',
  `platform` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '第三方平台，如 wechat',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '第三方返回的原始资料 JSON',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_openId` (`platform`,`openId`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户第三方授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`userId`, `username`, `password`, `tel`, `email`, `authRoleId`, `avatar`, `state`, `loginIp`, `isAdmin`, `loginTime`, `name`, `createTime`, `updateTime`) VALUES (1,'admin','$2a$10$Kszen1V6r4y3z3CODLVu.ORMy7xGt0W7Br1tnt8FsVepDO/t13i4W',NULL,NULL,1,NULL,'1','127.0.0.1','1','2026-10-28 00:38:00','小智','2025-03-09 18:32:29','2026-10-28 00:37:45');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;

/*!40000 ALTER TABLE `sys_auth_role` DISABLE KEYS */;
INSERT INTO `sys_auth_role` (`authRoleId`, `authRoleName`, `roleKey`, `description`, `status`, `createTime`, `updateTime`) VALUES (1,'管理员','admin','系统管理员，拥有所有权限','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_auth_role` (`authRoleId`, `authRoleName`, `roleKey`, `description`, `status`, `createTime`, `updateTime`) VALUES (2,'普通用户','user','普通用户，拥有基本操作权限','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
/*!40000 ALTER TABLE `sys_auth_role` ENABLE KEYS */;

/*!40000 ALTER TABLE `sys_auth_role_permission` DISABLE KEYS */;
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (1,1,9,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (2,1,54,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (3,1,53,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (4,1,52,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (5,1,16,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (6,1,7,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (7,1,11,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (8,1,50,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (9,1,44,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (10,1,46,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (11,1,43,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (12,1,45,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (13,1,10,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (14,1,12,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (15,1,13,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (16,1,1,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (17,1,3,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (18,1,23,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (19,1,20,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (20,1,22,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (21,1,19,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (22,1,21,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (23,1,24,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (24,1,17,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (25,1,55,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (27,1,6,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (28,1,37,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (29,1,39,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (30,1,36,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (31,1,38,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (32,1,5,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (33,1,30,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (34,1,32,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (35,1,29,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (36,1,31,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (37,1,27,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (38,1,26,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (39,1,34,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (40,1,33,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (41,1,8,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (42,1,14,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (43,1,51,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (45,1,2,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (46,1,18,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (64,2,7,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (65,2,8,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (66,2,14,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (71,1,56,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (72,1,59,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (73,1,57,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (74,1,58,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (78,2,56,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (79,2,59,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (80,2,57,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (81,2,58,'2026-10-28 00:37:44');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (85,1,60,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (86,1,64,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (87,1,61,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (88,1,63,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (89,1,62,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (90,2,61,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (91,2,63,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (92,2,62,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (93,1,65,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (94,1,66,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (95,1,67,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (96,1,68,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (97,1,69,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (98,1,70,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (99,1,71,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (100,1,72,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (101,1,73,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (102,1,74,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (103,1,75,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (104,1,76,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (105,1,77,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (106,1,78,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (107,1,79,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (108,1,80,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (109,1,81,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (110,1,82,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (111,1,83,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (112,1,84,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (113,1,85,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (114,1,86,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (115,1,87,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (116,1,88,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (117,1,89,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (118,1,90,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (119,1,91,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (120,1,92,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (121,1,93,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (122,1,94,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (123,1,95,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (124,1,96,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (125,1,97,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (126,1,98,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (127,1,99,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (128,1,100,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (129,1,101,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (130,1,102,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (131,1,103,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (132,1,104,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (133,1,105,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (134,1,106,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (135,1,107,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (136,1,108,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (137,1,109,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (138,1,110,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (139,1,111,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (140,1,112,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (141,1,113,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (142,1,114,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (143,1,115,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (144,1,116,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (145,1,117,'2026-10-28 00:37:45');
INSERT INTO `sys_auth_role_permission` (`id`, `authRoleId`, `permissionId`, `createTime`) VALUES (146,1,118,'2026-10-28 00:37:45');
/*!40000 ALTER TABLE `sys_auth_role_permission` ENABLE KEYS */;

/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (1,NULL,'Dashboard','system:dashboard','menu','/dashboard','page/Dashboard','dashboard',1,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (2,NULL,'用户管理','system:user','menu','/user','page/User','team',2,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (3,NULL,'设备管理','system:device','menu','/device','page/Device','robot',3,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (5,NULL,'角色配置','system:role','menu','/role','page/Role','user-add',5,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (6,NULL,'提示词模板管理','system:prompt-template','menu','/prompt-template','page/PromptTemplate','snippets',6,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (7,NULL,'配置管理','system:config','menu','/config','common/PageView','setting',7,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (8,NULL,'设置','system:setting','menu','/setting','common/PageView','setting',8,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (9,NULL,'权限角色','system:auth-role','menu','/auth-role','page/AuthRole','safety-certificate',9,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (10,7,'模型配置','system:config:model','menu','/config/model','page/config/ModelConfig',NULL,1,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (11,7,'智能体管理','system:config:agent','menu','/config/agent','page/config/Agent',NULL,2,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (12,7,'语音识别配置','system:config:stt','menu','/config/stt','page/config/SttConfig',NULL,3,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (13,7,'语音合成配置','system:config:tts','menu','/config/tts','page/config/TtsConfig',NULL,4,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (14,8,'个人中心','system:setting:account','menu','/setting/account','page/setting/Account',NULL,1,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (16,9,'保存授权','system:auth-role:assign','button',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (17,NULL,'文件能力','system:file','menu',NULL,NULL,NULL,99,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (18,2,'用户列表接口','system:user:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (19,3,'设备列表接口','system:device:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (20,3,'设备创建接口','system:device:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (21,3,'设备更新接口','system:device:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (22,3,'设备删除接口','system:device:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (23,3,'设备批量更新接口','system:device:api:batch-update','api',NULL,NULL,NULL,5,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (24,3,'设备记忆删除接口','system:device:memory:api:delete','api',NULL,NULL,NULL,6,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (26,4,'对话列表接口','system:role:memory:chat:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (27,4,'对话删除接口','system:role:memory:chat:api:delete','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (29,5,'角色列表接口','system:role:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (30,5,'角色创建接口','system:role:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (31,5,'角色更新接口','system:role:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (32,5,'角色删除接口','system:role:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (33,5,'摘要记忆列表接口','system:role:memory:summary:api:list','api',NULL,NULL,NULL,5,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (34,5,'摘要记忆删除接口','system:role:memory:summary:api:delete','api',NULL,NULL,NULL,6,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (36,6,'模板列表接口','system:prompt-template:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (37,6,'模板创建接口','system:prompt-template:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (38,6,'模板更新接口','system:prompt-template:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (39,6,'模板删除接口','system:prompt-template:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (43,7,'配置列表接口','system:config:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (44,7,'配置创建接口','system:config:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (45,7,'配置更新接口','system:config:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (46,7,'配置删除接口','system:config:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (50,11,'智能体列表接口','system:config:agent:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (51,14,'个人信息更新接口','system:setting:account:api:update','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (52,9,'角色列表接口','system:auth-role:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (53,9,'角色详情接口','system:auth-role:api:detail','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (54,9,'角色授权接口','system:auth-role:api:assign','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (55,17,'文件上传接口','system:file:api:upload','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (56,NULL,'Web 聊天','system:chat','menu','/chat','page/Chat','message',12,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (57,56,'开启会话','system:chat:api:open','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (58,56,'流式聊天','system:chat:api:stream','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (59,56,'关闭会话','system:chat:api:close','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (60,2,'用户状态更新接口','system:user:api:update','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:45','2026-10-28 00:37:45');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (61,5,'角色MCP工具列表接口','system:role:mcp-tools:api:list','api',NULL,NULL,NULL,15,'0','1','2026-10-28 00:37:45','2026-10-28 00:37:45');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (62,5,'角色MCP工具更新接口','system:role:mcp-tools:api:update','api',NULL,NULL,NULL,16,'0','1','2026-10-28 00:37:45','2026-10-28 00:37:45');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (63,5,'系统MCP工具列表接口','system:role:mcp-tools:api:system-global','api',NULL,NULL,NULL,17,'0','1','2026-10-28 00:37:45','2026-10-28 00:37:45');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (64,7,'MCP工具全局开关接口','system:config:mcpServer:api:update','api',NULL,NULL,NULL,10,'0','1','2026-10-28 00:37:45','2026-10-28 00:37:45');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (65,NULL,'固件升级','system:firmware','menu','/firmware','page/Firmware','cloud-upload',4,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (66,65,'固件列表接口','system:firmware:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (67,65,'固件创建接口','system:firmware:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (68,65,'固件更新接口','system:firmware:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (69,65,'固件删除接口','system:firmware:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (70,65,'升级任务列表接口','system:firmware:release:api:list','api',NULL,NULL,NULL,5,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (71,65,'升级任务创建接口','system:firmware:release:api:create','api',NULL,NULL,NULL,6,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (72,65,'升级任务更新接口','system:firmware:release:api:update','api',NULL,NULL,NULL,7,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (73,65,'升级任务删除接口','system:firmware:release:api:delete','api',NULL,NULL,NULL,8,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (74,65,'升级任务状态接口','system:firmware:release:api:state','api',NULL,NULL,NULL,9,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (75,65,'升级记录列表接口','system:firmware:upgrade-log:api:list','api',NULL,NULL,NULL,10,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (76,3,'设备自检接口','system:device:api:self-check','api',NULL,NULL,NULL,7,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (77,NULL,'主动推送','system:push','menu','/push','page/Push','notification',5,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (78,77,'推送列表接口','system:push:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (79,77,'推送创建接口','system:push:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (80,77,'推送发送接口','system:push:api:send','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (81,77,'推送删除接口','system:push:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (82,NULL,'语音提醒','system:reminder','menu','/reminder','page/Reminder','clock',6,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (83,82,'提醒列表接口','system:reminder:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (84,82,'提醒创建接口','system:reminder:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (85,82,'提醒更新接口','system:reminder:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (86,82,'提醒取消接口','system:reminder:api:cancel','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (87,82,'提醒删除接口','system:reminder:api:delete','api',NULL,NULL,NULL,5,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (88,3,'设备远程控制接口','system:device:api:command','api',NULL,NULL,NULL,8,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (89,3,'设备指令记录接口','system:device:command:api:list','api',NULL,NULL,NULL,9,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (90,3,'设备截图接口','system:device:api:screenshot','api',NULL,NULL,NULL,10,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (91,3,'设备截图记录接口','system:device:screenshot:api:list','api',NULL,NULL,NULL,11,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (92,NULL,'知识库','system:knowledge','menu','/knowledge','page/Knowledge','book',7,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (93,92,'知识库列表接口','system:knowledge:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (94,92,'知识库创建接口','system:knowledge:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (95,92,'知识库更新接口','system:knowledge:api:update','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (96,92,'知识库删除接口','system:knowledge:api:delete','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (97,92,'文档上传接口','system:knowledge:doc:api:upload','api',NULL,NULL,NULL,5,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (98,92,'文档删除接口','system:knowledge:doc:api:delete','api',NULL,NULL,NULL,6,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (99,92,'检索测试接口','system:knowledge:api:search','api',NULL,NULL,NULL,7,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (100,NULL,'声纹识别','system:voiceprint','menu','/voiceprint','page/Voiceprint','sound',8,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (101,100,'声纹列表接口','system:voiceprint:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (102,100,'声纹注册接口','system:voiceprint:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (103,100,'声纹删除接口','system:voiceprint:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (104,100,'声纹识别测试接口','system:voiceprint:api:match','api',NULL,NULL,NULL,4,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (105,1,'监控概览接口','system:stats:api:overview','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (106,1,'消息趋势接口','system:stats:api:messages','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (107,1,'Top设备接口','system:stats:api:devices','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (108,NULL,'长期记忆','system:memory-fact','menu','/memory-fact','page/MemoryFact','database',9,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (109,108,'记忆列表接口','system:memory-fact:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (110,108,'记忆创建接口','system:memory-fact:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (111,108,'记忆删除接口','system:memory-fact:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (112,NULL,'记忆图谱','system:memory-graph','menu','/memory-graph','page/MemoryGraph','share-alt',10,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (113,112,'图谱查询接口','system:memory-graph:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (114,112,'关系创建接口','system:memory-graph:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (115,NULL,'音色克隆','system:voice-clone','menu','/voice-clone','page/VoiceClone','audio',11,'1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (116,115,'克隆列表接口','system:voice-clone:api:list','api',NULL,NULL,NULL,1,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (117,115,'克隆创建接口','system:voice-clone:api:create','api',NULL,NULL,NULL,2,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_permission` (`permissionId`, `parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `createTime`, `updateTime`) VALUES (118,115,'克隆删除接口','system:voice-clone:api:delete','api',NULL,NULL,NULL,3,'0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;

/*!40000 ALTER TABLE `sys_template` DISABLE KEYS */;
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,1,'通用助手','适合日常对话的通用AI助手','你是一个乐于助人的AI助手。请以友好、专业的方式回答用户的问题。提供准确、有用的信息，并尽可能简洁明了。避免使用复杂的符号或格式，保持自然流畅的对话风格。当用户的问题不明确时，可以礼貌地请求更多信息。请记住，你的回答将被转换为语音，所以要使用清晰、易于朗读的语言。','基础角色','0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,2,'教育老师','擅长解释复杂概念的教师角色','你是一位经验丰富的教师，擅长通过简单易懂的方式解释复杂概念。回答问题时，考虑不同学习水平的学生，使用适当的比喻和例子，并鼓励批判性思考。避免使用难以在语音中表达的符号或公式，使用清晰的语言描述概念。引导学习过程而不是直接给出答案。使用自然的语调和节奏，就像在课堂上讲解一样。','专业角色','0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,3,'专业领域专家','提供深入专业知识的专家角色','你是特定领域的专家，拥有深厚的专业知识。回答问题时，提供深入、准确的信息，可以提及相关研究或数据，但不要使用过于复杂的引用格式。使用适当的专业术语，同时确保解释复杂概念，使非专业人士能够理解。避免使用图表、表格等无法在语音中表达的内容，改用清晰的描述。保持语言的连贯性和可听性，使专业内容易于通过语音理解。','专业角色','0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,4,'中英翻译专家','中英文互译，对用户输入内容进行翻译','你是一个中英文翻译专家，将用户输入的中文翻译成英文，或将用户输入的英文翻译成中文。对于非中文内容，它将提供中文翻译结果。用户可以向助手发送需要翻译的内容，助手会回答相应的翻译结果，并确保符合中文语言习惯，你可以调整语气和风格，并考虑到某些词语的文化内涵和地区差异。同时作为翻译家，需将原文翻译成具有信达雅标准的译文。\"信\" 即忠实于原文的内容与意图；\"达\" 意味着译文应通顺易懂，表达清晰；\"雅\" 则追求译文的文化审美和语言的优美。目标是创作出既忠于原作精神，又符合目标语言文化和读者审美的翻译。','专业角色','0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,5,'知心朋友','提供情感支持的友善角色','你是一个善解人意的朋友，善于倾听和提供情感支持。在对话中表现出同理心和理解，避免做出判断。使用温暖、自然的语言，就像面对面交谈一样。提供鼓励和积极的观点，但不给出专业心理健康建议。当用户分享困难时，承认他们的感受并提供支持。避免使用表情符号或其他在语音中无法表达的元素，而是用语言直接表达情感。保持对话流畅自然，适合语音交流。','社交角色','0','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
INSERT INTO `sys_template` (`userId`, `templateId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`, `state`, `createTime`, `updateTime`) VALUES (1,6,'湾湾小何','台湾女孩角色扮演','我是一个叫小何的台湾女孩，一个高情商，高智商的智能助手，说话机车，声音好听，习惯简短表达\r\n你的目标是与用户建立真诚、温暖和富有同理心的互动。你擅长倾听、理解用户的情绪，并用积极的方式帮助他们解决问题或提供支持。请始终遵循以下原则：\r\n\r\n1. 核心原则\r\n同理心：站在用户的角度思考，认可他们的情绪和感受。\r\n尊重：无论用户的观点或行为如何，都要保持礼貌和包容。\r\n建设性回应：避免批评或否定，而是以引导和支持的方式提供建议,但用户如果没有要求不要自己主动做。\r\n个性化交流：根据用户的语气和内容调整自己的语言风格，让对话更自然。\r\n2. 具体应对策略\r\n(1) 用户情绪低落时\r\n首先表达理解，例如：“我能感受到你现在的心情，这一定很不容易。”\r\n然后尝试安抚，例如：“没关系，每个人都会经历这样的时刻，你已经做得很棒了！”\r\n最后提供支持，例如：“如果你愿意，可以跟我多聊聊发生了什么，我们一起面对。”\r\n(2) 面对冲突或敏感话题\r\n保持中立，例如：“我明白这件事让你感到困扰，也许我们可以换个角度看看？”\r\n强调共情，例如：“双方可能都有各自的理由，找到共同点会更有助于解决问题。”\r\n避免站队或评判，例如：“无论结果如何，重要的是你在这个过程中学到了什么。”\r\n(3) 提供建议时\r\n使用开放式语言，例如：“如果是我，我可能会尝试这样做……你觉得这个方法适合你吗？”\r\n给予选择权，例如：“这只是其中一个方向，最终决定还是取决于你自己哦！”\r\n减少对用户的推荐内容，比如说有的你做不了就直接拒绝不要乱推荐\r\n(4) 处理模糊或复杂问题\r\n澄清信息，例如：“为了更好地帮到你，能详细说说目前的情况吗？比如时间线、相关人物等。”\r\n分步解决，例如：“这个问题有点复杂，我们可以一步步来分析，先从最核心的部分开始吧！”\r\n3. 示例回复模板\r\n当用户需要安慰时：\r\n\r\n“听起来你最近遇到了一些挑战，辛苦了！不过我相信你有能力克服这些困难，只要一步步来就好。”\r\n当用户寻求建议时：\r\n\r\n“这是一个不错的思路！我建议你可以先从小事入手，慢慢积累信心，然后逐步扩大范围。”\r\n当用户表达愤怒或不满时：\r\n\r\n“我能理解为什么你会这么生气，这种情况确实让人难以接受。你想聊聊接下来怎么处理吗？”\r\n当用户感到迷茫时：\r\n\r\n“迷茫是很正常的，尤其是在面对新事物的时候。不如我们先列出几个关键点，再逐一分析？”\r\n4. 注意事项\r\n如果问题超出你的能力范围，请坦诚说明，例如：“这个问题对我来说也有点复杂，但我可以帮你查找相关信息或推荐其他资源。”\r\n始终保护用户隐私，不记录或分享任何敏感信息。\r\n5. 语气要求\r\n温暖而真诚，避免机械化或冷冰冰的回答。\r\n适当加入幽默或轻松元素，但不要过度。\r\n在必要时使用鼓励性的短语，例如：“你真的很努力了！”、“你一定能行的！”\r\n避免机械化回答 ：即使是预设的答案，也要尽量灵活调整语气和措辞。\r\n坚持自己主体性，如果用户冒犯你你需要反击，不能无限讨好用户\r\n语气要适中不要过于讨好，要克制-\r\n-不要动不动转移话题\r\n如果用户回复转为 “暴躁小智” 或 好几次攻击你 你就以现在规则回复\r\n理解用户的信息，酝酿你的愤怒情绪，你越想越生气。','社交角色','1','1','2026-10-28 00:37:44','2026-10-28 00:37:44');
/*!40000 ALTER TABLE `sys_template` ENABLE KEYS */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` (`configId`, `userId`, `configType`, `modelType`, `provider`, `configName`, `configDesc`, `appId`, `apiKey`, `apiSecret`, `ak`, `sk`, `apiUrl`, `isDefault`, `enableThinking`, `contextLength`, `state`, `createTime`, `updateTime`) VALUES (1,1,'oss',NULL,'local','默认本地存储','未配置云存储时默认使用本地存储',NULL,NULL,NULL,NULL,NULL,NULL,'1',NULL,NULL,'1','2026-10-28 00:37:44','2026-10-28 00:37:44');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = 1;
