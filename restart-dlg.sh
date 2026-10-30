#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose restart dialogue >/dev/null 2>&1
sleep 55
echo '=== 重启后 MQTT 日志 ==='
docker compose logs --since 2m dialogue 2>&1 | grep -iE 'mqtt' | tail -5
echo '=== broker 客户端 ==='
docker compose exec -T emqx /opt/emqx/bin/emqx ctl clients list 2>&1 | head -3

