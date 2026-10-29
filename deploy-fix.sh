#!/usr/bin/env bash
set -e
cd /opt/xiaozhi
echo "=== 修正 tag（workflow 产出的是 6.2.0，不带 v）==="
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.0/' .env
grep '^XIAOZHI_TAG=' .env
echo "=== 拉取 ==="
docker compose pull 2>&1 | tail -8
echo "=== 启动 ==="
docker compose up -d 2>&1 | tail -8
sleep 35
docker compose ps --format '{{.Name}} | {{.Image}} | {{.Status}}'
echo "=== 增量建表 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < upgrade-firmware.sql && echo "upgrade-sql ok"
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=\"xiaozhi\" AND table_name LIKE \"sys_%\" "'
echo "=== 清权限缓存 ==="
docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done; echo "cache cleared"
