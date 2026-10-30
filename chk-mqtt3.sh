#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== MQTT 全部日志（近 10 分钟）===' 
docker compose logs --since 10m dialogue 2>&1 | grep -iE 'mqtt' | tail -8
echo '=== emqx 已连接客户端 ==='
docker compose exec -T emqx /opt/emqx/bin/emqx ctl clients list 2>&1 | head -5

