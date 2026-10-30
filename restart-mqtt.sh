#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== 当前版本 ==='
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo '=== 重启 server/dialogue 让 MQTT 生效 ==='
docker compose up -d server dialogue 2>&1 | tail -3
sleep 55
echo '=== MQTT 连接日志 ==='
docker compose logs --since 3m dialogue 2>&1 | grep -iE 'mqtt|MQTT' | tail -8
echo '=== 容器状态 ==='
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
