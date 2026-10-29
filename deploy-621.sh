#!/usr/bin/env bash
set -e
cd /opt/xiaozhi
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.1/' .env
grep '^XIAOZHI_TAG=' .env
docker compose pull 2>&1 | tail -4
docker compose up -d 2>&1 | tail -4
sleep 40
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
