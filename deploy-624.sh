#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.4/' .env
echo "[$(date +%T)] tag=$(grep '^XIAOZHI_TAG=' .env)"
for i in $(seq 1 10); do
  echo "[$(date +%T)] pull attempt $i"
  if docker compose pull 2>&1 | tail -4 | tee -a /tmp/deploy-624.log; then
    docker compose up -d 2>&1 | tail -5
    sleep 40
    docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
    exit 0
  fi
  sleep 120
done
echo 'pull 一直失败（镜像可能还没构建完）'
