#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== 容器内 MQTT 环境变量 ==='
docker compose exec -T dialogue printenv | grep -i mqtt || echo none
echo '=== .env ==='
grep '^XIAOZHI_MQTT' .env
