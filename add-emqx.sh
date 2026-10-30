#!/usr/bin/env bash
set -e
cd /opt/xiaozhi
cp -f docker-compose.yml docker-compose.yml.bak-emqx
echo '=== 校验 compose ==='
docker compose config -q && echo config-ok
echo '=== 打开 MQTT 开关 ==='
grep -q '^XIAOZHI_MQTT_ENABLED=' .env && sed -i 's|^XIAOZHI_MQTT_ENABLED=.*|XIAOZHI_MQTT_ENABLED=true|' .env || echo 'XIAOZHI_MQTT_ENABLED=true' >> .env
grep -q '^XIAOZHI_MQTT_BROKER=' .env && sed -i 's|^XIAOZHI_MQTT_BROKER=.*|XIAOZHI_MQTT_BROKER=tcp://emqx:1883|' .env || echo 'XIAOZHI_MQTT_BROKER=tcp://emqx:1883' >> .env
grep '^XIAOZHI_MQTT' .env
echo '=== 拉 emqx ==='
docker compose pull emqx 2>&1 | tail -2
docker compose up -d emqx 2>&1 | tail -2
sleep 50
docker compose ps --format '{{.Name}} {{.Status}}'
