#!/usr/bin/env bash
set -e
cd /opt/xiaozhi
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.3/' .env
grep '^XIAOZHI_TAG=' .env
docker compose pull 2>&1 | tail -3
docker compose up -d 2>&1 | tail -3
sleep 45
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
