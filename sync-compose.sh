#!/usr/bin/env bash
cd /opt/xiaozhi
mkdir -p music
cp -f docker-compose.yml docker-compose.yml.bak-music 2>/dev/null || true
echo '=== 目录 ==='
ls -ld music
echo '=== 当前容器版本 ==='
docker compose ps --format '{{.Name}} {{.Image}}' | head -4

