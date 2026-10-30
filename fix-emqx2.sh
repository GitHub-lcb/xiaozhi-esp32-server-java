#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose up -d emqx server dialogue 2>&1 | tail -4
sleep 55
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo '=== broker 端口自检 ==='
curl -s -o /dev/null -w 'dashboard=%{http_code}\n' http://127.0.0.1:18083/status || true
echo '=== MQTT 日志 ==='
docker compose logs --since 4m dialogue 2>&1 | grep -iE 'mqtt' | tail -8

