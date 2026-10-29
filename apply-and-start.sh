#!/usr/bin/env bash
cd /opt/xiaozhi
cp -f /tmp/supplement.sql ./supplement.sql
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < supplement.sql 2>/dev/null
echo "sql-applied"
echo "tables=$(docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()" xiaozhi' 2>/dev/null)"
echo "new-perms=$(docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM sys_permission WHERE permissionId>=100" xiaozhi' 2>/dev/null)"
docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done
echo "cache-cleared"
docker compose restart server dialogue >/dev/null 2>&1
sleep 45
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
