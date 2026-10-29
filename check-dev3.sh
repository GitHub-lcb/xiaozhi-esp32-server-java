#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo '=== 通道表列 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=(select min(table_name) from information_schema.tables where table_schema=DATABASE() and table_name like char(115,121,115,95,100,101,118,105,99,101,95,99,104,97,110,110,101,108))"' 2>/dev/null | tr '\n' ' '
echo ''
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT * FROM sys_device_channel"' 2>/dev/null | head -6
echo '=== 该设备近期 WS 连接日志 ==='
docker compose logs --since 2h dialogue 2>&1 | grep -c '7c:4f:ad:21:8f:34' || true
docker compose logs --since 2h dialogue 2>&1 | grep '7c:4f:ad:21:8f:34' | tail -5
