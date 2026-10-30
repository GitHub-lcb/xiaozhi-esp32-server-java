#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose ps emqx --format '{{.Name}} {{.Status}}'
echo '=== emqx 日志尾部 ==='
docker compose logs --tail 25 emqx 2>&1 | tail -25
echo '=== 健康检查输出 ==='
docker inspect --format '{{json .State.Health}}' $(docker compose ps -q emqx) 2>/dev/null | head -c 600
echo ''
echo '=== 1883 端口 ==='
docker compose exec -T emqx sh -c 'nc -z 127.0.0.1 1883 && echo mqtt-port-ok' 2>/dev/null || echo no-nc
