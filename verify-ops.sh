#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose up -d server dialogue 2>&1 | tail -3
sleep 45
docker compose ps --format '{{.Name}} {{.Status}}'
echo '--- env in container ---'
docker compose exec -T server printenv XIAOZHI_OPS_TOKEN | sed 's/./x/g'
echo '--- prometheus ---'
docker compose exec -T server sh -c 'curl -s -o /dev/null -w "with-token=%{http_code} " -H "X-Ops-Token: $XIAOZHI_OPS_TOKEN" http://127.0.0.1:8091/actuator/prometheus; curl -s -o /dev/null -w "no-token=%{http_code}\n" http://127.0.0.1:8091/actuator/prometheus'
