#!/usr/bin/env bash
cd /opt/xiaozhi
echo "=== 1. 加列 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < /tmp/tts-first-chunk.sql 2>/dev/null
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SHOW COLUMNS FROM sys_dialogue_metric"' 2>/dev/null | grep ttsFirstChunk
echo "=== 2. 配置 ops token ==="
if grep -q '^XIAOZHI_OPS_TOKEN=.' .env; then
  echo "already set"
else
  TOKEN=$(openssl rand -hex 24)
  if grep -q '^XIAOZHI_OPS_TOKEN=' .env; then
    sed -i "s|^XIAOZHI_OPS_TOKEN=.*|XIAOZHI_OPS_TOKEN=$TOKEN|" .env
  else
    echo "XIAOZHI_OPS_TOKEN=$TOKEN" >> .env
  fi
  echo "token generated: ${TOKEN:0:8}..."
fi
echo "=== 3. 重启 server ==="
docker compose up -d server 2>&1 | tail -2
sleep 25
echo "=== 4. 验证 ==="
docker compose exec -T server sh -c 'T=$(printenv XIAOZHI_OPS_TOKEN); curl -s -o /dev/null -w "with-token=%{http_code}\n" -H "X-Ops-Token: $T" http://127.0.0.1:8091/actuator/prometheus; curl -s -o /dev/null -w "no-token=%{http_code}\n" http://127.0.0.1:8091/actuator/prometheus'
docker compose ps --format '{{.Name}} {{.Status}}'
