#!/usr/bin/env bash
cd /opt/xiaozhi
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=char(115,121,115,95,97,117,116,104,95,114,111,108,101,95,112,101,114,109,105,115,115,105,111,110)"' 2>/dev/null | tr '\n' ' '
echo ''
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT COUNT(*) FROM sys_music"' 2>/dev/null
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT COUNT(*) FROM sys_permission WHERE permissionKey LIKE char(115,121,115,116,101,109,58,102,105,108,101,37)"' 2>/dev/null
