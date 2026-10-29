#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < /tmp/supplement.sql 2>/dev/null
echo "supplement applied"
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()" xiaozhi'
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM sys_permission WHERE permissionId>=100" xiaozhi'
docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done
echo "cache cleared"
