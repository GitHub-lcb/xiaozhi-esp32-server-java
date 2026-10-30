#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
echo "[$(date +%T)] 等待三个镜像 6.2.10"
for i in $(seq 1 20); do
  ok=1
  for img in server dialogue web; do
    docker pull "ghcr.io/github-lcb/xiaozhi-$img:6.2.10" >/tmp/pull6210-$img.log 2>&1 || ok=0
  done
  if [ $ok -eq 1 ]; then
    echo "[$(date +%T)] 三个镜像就绪"
    sed -i 's/^XIAOZHI_TAG=.*/XIAOZHI_TAG=6.2.10/' .env
    docker compose up -d 2>&1 | tail -4
    sleep 45
    for f in /tmp/music_permissions.sql /tmp/music_library.sql; do
      echo "--- $f"
      docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < "$f" 2>&1 | grep -v Warning
    done
    docker compose exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' | while read k; do docker compose exec -T redis redis-cli del "$k" >/dev/null; done
    echo cache-cleared
    docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
    exit 0
  fi
  echo "[$(date +%T)] 第 $i 次: $(for img in server dialogue web; do echo -n "$img:$(tail -1 /tmp/pull6210-$img.log | cut -c1-60) "; done)"
  sleep 180
done
echo '等镜像超时'
