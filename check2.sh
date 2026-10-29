#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo "=== metric 行数 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT COUNT(*) FROM sys_dialogue_metric"' 2>/dev/null
echo "=== 最近 3 轮消息 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT messageId, deviceId, LEFT(content,40), createTime FROM sys_message ORDER BY messageId DESC LIMIT 6"' 2>/dev/null
echo "=== dialogue 容器 TTS 相关日志 ==="
docker compose logs --since 5h dialogue 2>&1 | grep -iE "TTS|合成|Speech|流式|error" | tail -15
echo "=== ops token 是否配置 ==="
grep -c '^XIAOZHI_OPS_TOKEN=.' .env || true
echo "=== prometheus 指标 ==="
docker compose exec -T server sh -c 'wget -qO- --header="X-Ops-Token: $XIAOZHI_OPS_TOKEN" http://127.0.0.1:8091/actuator/prometheus 2>/dev/null || curl -s -H "X-Ops-Token: $XIAOZHI_OPS_TOKEN" http://127.0.0.1:8091/actuator/prometheus' 2>/dev/null | grep -E "first_chunk|tts_" | head -10
