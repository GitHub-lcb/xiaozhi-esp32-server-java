#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo "=== sys_dialogue_metric 结构 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SHOW COLUMNS FROM sys_dialogue_metric"' 2>/dev/null | awk '{print $1}' | tr '\n' ' '
echo ""
echo "=== 最近 5 条对话指标 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT * FROM sys_dialogue_metric ORDER BY id DESC LIMIT 5\G"' 2>/dev/null | head -60
echo "=== 最近消息数 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT COUNT(*), MAX(createTime) FROM sys_message"' 2>/dev/null
echo "=== server 日志里的 TTS 线索 ==="
docker compose logs --since 4h server 2>&1 | grep -iE "tts|合成|first" | tail -12
