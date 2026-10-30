#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== 最近 3 分钟 MQTT 日志 ==='
docker compose logs --since 3m dialogue 2>&1 | grep -iE 'mqtt' | tail -6
echo '=== 通道表 ==='
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT deviceId, channel, onlineState, lastSeenTime FROM sys_device_channel LIMIT 5"' 2>/dev/null
echo '=== broker 连接数（dashboard API）===' 
curl -s -u admin:xiaozhi2024 'http://127.0.0.1:18083/api/v5/clients?limit=5' | head -c 300

