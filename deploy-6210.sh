#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
IMG=ghcr.io/github-lcb/xiaozhi-web:6.2.10
echo "[$(date +%T)] 等待镜像 $IMG"
for i in $(seq 1 15); do
  if docker pull "$IMG" >/tmp/pull6210.log 2>&1; then
    echo "[$(date +%T)] 镜像就绪，开始部署"
    sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.10/' .env
    docker compose pull 2>&1 | tail -3
    docker compose up -d 2>&1 | tail -3
    sleep 45
    echo '=== 执行升级脚本 ==='
    for f in /tmp/music_permissions.sql /tmp/music_library.sql; do
      [ -f "$f" ] && docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < "$f" && echo "ok: $f"
    done
    echo '=== 清权限缓存 ==='
    docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done
    echo cache-cleared
    docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
    exit 0
  fi
  echo "[$(date +%T)] 第 $i 次: $(tail -1 /tmp/pull6210.log)"
  sleep 180
done
echo '等镜像超时'
