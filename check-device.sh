#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo '=== 设备状态 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT deviceId, deviceName, state, lastConnectTime, updateTime FROM sys_device\G"' 2>/dev/null | head -40
echo '=== 通道表 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT * FROM sys_device_channel\G"' 2>/dev/null | head -30
echo '=== dialogue 最近连接日志 ==='
docker compose logs --since 3h dialogue 2>&1 | grep -iE '连接|在线|离线|connect|close' | tail -12
