#!/usr/bin/env bash
cd /opt/xiaozhi
echo "=== 迁移前表数 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=char(120,105,97,111,122,104,105)"'
echo "=== 执行增量脚本 ==="
[ -f upgrade-firmware.sql ] && docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < upgrade-firmware.sql && echo "sql done"
echo "=== 迁移后表数 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=char(120,105,97,111,122,104,105)"'
echo "=== 新权限行数 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM sys_permission WHERE permissionKey LIKE char(115,121,115,116,101,109,58)%"' xiaozhi 2>/dev/null
echo "=== 新表抽查 ==="
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SHOW TABLES LIKE char(37,102,105,114,109,119,97,114,101,37)" xiaozhi'
echo "=== 清权限缓存 ==="
docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done
echo "cache cleared"
echo "=== 服务健康 ==="
docker compose ps --format '{{.Name}} {{.Status}}'
