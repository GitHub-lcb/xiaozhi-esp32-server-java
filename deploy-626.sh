#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.6/' .env
echo "[$(date +%T)] tag=$(grep '^XIAOZHI_TAG=' .env)"
for i in $(seq 1 12); do
  echo "[$(date +%T)] pull $i"
  if docker compose pull 2>&1 | tail -3; then
    docker compose up -d 2>&1 | tail -3
    sleep 45
    docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
    echo '=== MQTT ==='
    docker compose logs --since 2m dialogue 2>&1 | grep -iE 'MQTT 通道' | tail -2
    exit 0
  fi
  sleep 120
done
echo 'pull failed after retries'
