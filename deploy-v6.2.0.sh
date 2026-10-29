#!/usr/bin/env bash
set -e
cd /opt/xiaozhi
echo "=== 1. 备份数据库 ==="
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines xiaozhi' > backup-before-v6.2.0.sql
ls -lh backup-before-v6.2.0.sql

echo "=== 2. 更新 compose 与升级脚本 ==="
cp -f docker-compose.yml docker-compose.yml.bak
curl -sH "Accept: application/vnd.github.raw" "https://api.github.com/repos/GitHub-lcb/xiaozhi-esp32-server-java/contents/docker-compose.yml?ref=v6.2.0" -o docker-compose.yml
curl -sH "Accept: application/vnd.github.raw" "https://api.github.com/repos/GitHub-lcb/xiaozhi-esp32-server-java/contents/db/upgrade/2026-10-28__firmware.sql?ref=v6.2.0" -o upgrade-firmware.sql
head -2 upgrade-firmware.sql
grep -c "CREATE TABLE" upgrade-firmware.sql

echo "=== 3. 切换镜像标签 ==="
if grep -q '^XIAOZHI_TAG=' .env; then sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=v6.2.0/' .env; else echo 'XIAOZHI_TAG=v6.2.0' >> .env; fi
grep '^XIAOZHI_TAG=' .env

echo "=== 4. 拉取并启动 ==="
docker compose pull
docker compose up -d
sleep 30
docker compose ps --format '{{.Name}} | {{.Image}} | {{.Status}}'
