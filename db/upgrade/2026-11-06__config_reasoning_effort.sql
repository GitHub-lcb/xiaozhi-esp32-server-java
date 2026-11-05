-- 2026-11-06 模型配置支持「思考强度」下拉（reasoningEffort）。
-- 背景：之前只有 enableThinking 开关，强度写死在 provider 里（DeepSeek=low、OpenAI=medium、火山=high），
--       后台无法按模型调。新增列后可配置 none/low/medium/high/max，留空则沿用提供方默认。
-- 约定：幂等（先查 information_schema 再决定是否 ALTER，可重复执行）；只加列不删列，不动已有数据。
-- 新装环境直接执行 db/init.sql 即可，无需本脚本。

SET NAMES utf8mb4;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'reasoningEffort'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `sys_config` ADD COLUMN `reasoningEffort` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''思考强度(none/low/medium/high/max，空则用提供方默认)'' AFTER `enableThinking`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
