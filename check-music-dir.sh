#!/usr/bin/env bash
cd /opt/xiaozhi
tail -4 /tmp/deploy-628.out
echo '=== 容器 ==='
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo '=== 音乐卷 ==='
docker volume ls --format '{{.Name}}' | grep -i music || echo 'no music volume yet'
echo '=== 容器内音乐目录 ==='
docker compose exec -T dialogue sh -c 'ls -la /app/uploads/music 2>/dev/null | head -5' || echo 'dir missing'
