-- TTS 首块音频耗时：流式改造的效果指标，随每轮对话落库
-- 幂等：列已存在时跳过
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'sys_dialogue_metric'
                      AND column_name = 'ttsFirstChunkMs');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE sys_dialogue_metric ADD COLUMN ttsFirstChunkMs bigint DEFAULT NULL COMMENT ''TTS首块音频耗时(ms)''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
