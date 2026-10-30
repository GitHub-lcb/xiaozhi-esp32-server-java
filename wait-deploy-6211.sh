#!/usr/bin/env bash
cd /opt/xiaozhi || exit 1
echo "[$(date +%T)] 等待 v6.2.11 镜像构建完成"
for i in $(seq 1 20); do
  ok=1
  for img in server dialogue web; do
    docker pull "ghcr.io/github-lcb/xiaozhi-$img:6.2.11" >/dev/null 2>&1 || ok=0
  done
  if [ $ok -eq 1 ]; then
    echo "[$(date +%T)] 镜像齐备，开始部署"
    bash ./deploy.sh 6.2.11
    exit $?
  fi
  echo "[$(date +%T)] 第 $i 次：镜像未就绪"
  sleep 180
done
echo '等镜像超时'
