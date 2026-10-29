#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N e="SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name" xiaozhi' > /tmp/tables.txt 2>/dev/null
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name" xiaozhi' | tr '\n' ' '
echo ""
echo "count=$(docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()" xiaozhi')"
