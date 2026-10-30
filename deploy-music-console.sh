#!/usr/bin/env bash
# 部署曲库管理台版本并自动修数据库权限（在服务器 /opt/xiaozhi 上执行）
#
# 用法：
#   cd /opt/xiaozhi
#   bash deploy-music-console.sh 6.2.11      # 参数是本次发布的镜像 tag
#
# 做的事：拉镜像 → 改 XIAOZHI_TAG → compose up -d → 跑曲库幂等升级 SQL
#        → 清 Redis 权限缓存 → 打容器状态。
# 前提：CI 已按 v<tag> 打标签并发布 ghcr.io/github-lcb/xiaozhi-{server,dialogue,web}:<tag>
set -euo pipefail

TAG="${1:?用法: bash deploy-music-console.sh <镜像tag，例如 6.2.11>}"
cd /opt/xiaozhi 2>/dev/null || { echo "找不到 /opt/xiaozhi"; exit 1; }

IMG="ghcr.io/github-lcb/xiaozhi-web:${TAG}"
echo "[$(date +%T)] 拉取镜像 ${TAG}（最多等 45 分钟，镜像构建慢时先等 CI 完成）"
for i in $(seq 1 15); do
  if docker pull "$IMG" >/tmp/pull-music-console.log 2>&1; then
    echo "[$(date +%T)] 镜像就绪"
    break
  fi
  echo "[$(date +%T)] 第 $i 次: $(tail -1 /tmp/pull-music-console.log)"
  if [ "$i" -eq 15 ]; then
    echo "等镜像超时，确认 tag 是否已发布：ghcr.io/github-lcb/xiaozhi-web:${TAG}"
    exit 1
  fi
  sleep 180
done

sed -i "s/^XIAOZHI_TAG=.*/XIAOZHI_TAG=${TAG}/" .env
docker compose pull 2>&1 | tail -3
docker compose up -d 2>&1 | tail -3
sleep 30

echo "[$(date +%T)] 执行曲库升级 SQL（幂等）"
# 复用 fix 脚本里的 SQL：直接调用它，省得两处维护
bash ./fix-music-permissions.sh

echo "[$(date +%T)] 容器状态"
docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo "部署完成：后台强刷页面并重新登录后再进「智能体 → 曲库」"
