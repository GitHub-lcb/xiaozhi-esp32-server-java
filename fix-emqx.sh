#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose up -d emqx 2>&1 | tail -3
sleep 40
docker compose ps emqx --format '{{.Name}} {{.Status}}'
echo '=== 重启 server/dialogue ==='
docker compose up -d server dialogue 2>&1 | tail -3
sleep 50
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo '=== MQTT 日志 ==='
docker compose logs --since 3m dialogue 2>&1 | grep -iE 'mqtt' | tail -6
