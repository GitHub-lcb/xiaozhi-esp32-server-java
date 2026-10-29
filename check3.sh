#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo "=== 最近 8 条 latencyMs ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT createTime, latencyMs, totalTokens FROM sys_dialogue_metric ORDER BY metricId DESC LIMIT 8"' 2>/dev/null
echo "=== 延迟分布 ==="
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT MIN(latencyMs), AVG(latencyMs), MAX(latencyMs), COUNT(*) FROM sys_dialogue_metric"' 2>/dev/null
echo "=== ops token ==="
docker compose exec -T server printenv XIAOZHI_OPS_TOKEN 2>/dev/null | head -1 | sed 's/./x/g'
echo "=== prometheus first_chunk ==="
docker compose exec -T server sh -c 'T=$(printenv XIAOZHI_OPS_TOKEN); curl -s -H "X-Ops-Token: $T" http://127.0.0.1:8091/actuator/prometheus' 2>/dev/null | grep -E "tts" | head -8
