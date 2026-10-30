#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
IMG=ghcr.io/github-lcb/xiaozhi-web:6.2.8
echo "[$(date +%T)] 等待镜像 $IMG"
for i in $(seq 1 15); do
  if docker pull "$IMG" >/tmp/pull628.log 2>&1; then
    echo "[$(date +%T)] 镜像就绪，开始部署"
    sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.8/' .env
    docker compose pull 2>&1 | tail -3
    docker compose up -d 2>&1 | tail -3
    sleep 50
    docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
    echo '=== MQTT ==='
    docker compose logs --since 3m dialogue 2>&1 | grep -iE 'MQTT 通道' | tail -2
    exit 0
  fi
  echo "[$(date +%T)] 第 $i 次: $(tail -1 /tmp/pull628.log)"
  sleep 180
done
echo '等镜像超时'
