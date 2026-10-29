#!/usr/bin/env bash
cd /opt/xiaozhi
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.0/' .env
docker compose pull >/tmp/pull.log 2>&1; echo "pull exit=$?"
docker compose up -d >/tmp/up.log 2>&1; echo "up exit=$?"
sleep 20
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo "--- migrate ---"
if [ -f upgrade-firmware.sql ]; then docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < upgrade-firmware.sql; echo "sql exit=$?"; fi
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SHOW TABLES" xiaozhi' | wc -l
echo "--- cache ---"
docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | xargs -r -n1 -I{} docker compose exec -T redis redis-cli del "{}" >/dev/null 2>&1; echo "cache done"
