#!/usr/bin/env bash
cd /opt/xiaozhi
M='docker compose exec -T mysql sh -c'
echo '=== sys_device 列 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -N -e "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=\"sys_device\""' 2>&1 | tr '\n' ' '
echo ''
echo '=== 设备状态 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT deviceId, deviceName, state, updateTime FROM sys_device"' 2>&1 | head -10
echo '=== 通道 ==='
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT deviceId, channel, online, updateTime FROM sys_device_channel"' 2>&1 | head -10
