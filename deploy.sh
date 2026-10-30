#!/usr/bin/env bash
# 一键发布：./deploy.sh 6.2.11
# 回滚：    ./deploy.sh --rollback
# 预演：    ./deploy.sh 6.2.11 --dry-run
#
# 放在服务器 /opt/xiaozhi 下执行（与 docker-compose.yml、.env 同级）。
set -uo pipefail

VERSION="${1:-}"
MODE="${2:-}"
COMPOSE="docker compose"
ENV_FILE=".env"
BACKUP=".env.backup-$(date +%Y%m%d-%H%M%S)"
HEALTH_TIMEOUT=180

log()  { echo "[$(date +%T)] $*"; }
fail() { echo "[$(date +%T)] ❌ $*" >&2; exit 1; }

# ---------- 回滚 ----------
if [ "$VERSION" = "--rollback" ]; then
  LATEST_BACKUP=$(ls -1t .env.backup-* 2>/dev/null | head -1)
  [ -n "$LATEST_BACKUP" ] || fail "找不到 .env 备份，无法回滚"
  log "回滚到 $LATEST_BACKUP"
  cp -f "$LATEST_BACKUP" "$ENV_FILE"
  $COMPOSE up -d || fail "回滚重建失败"
  $COMPOSE ps --format '{{.Name}} {{.Image}} {{.Status}}'
  exit 0
fi

[ -n "$VERSION" ] || fail "用法: ./deploy.sh <版本号，如 6.2.11> [--dry-run|--rollback]"
[ -f docker-compose.yml ] || fail "当前目录没有 docker-compose.yml"
[ -f "$ENV_FILE" ] || fail "当前目录没有 .env"

# ---------- 1. 镜像必须三个都齐 ----------
log "检查镜像 ghcr.io/github-lcb/xiaozhi-{server,dialogue,web}:$VERSION"
for img in server dialogue web; do
  if ! docker pull "ghcr.io/github-lcb/xiaozhi-$img:$VERSION" >/dev/null 2>&1; then
    fail "镜像 xiaozhi-$img:$VERSION 不存在或拉取失败；先确认 CI 已构建完成（Actions → docker）"
  fi
  log "  ✓ xiaozhi-$img:$VERSION"
done

if [ "$MODE" = "--dry-run" ]; then
  log "预演结束：镜像齐备，未做任何变更"
  exit 0
fi

# ---------- 2. 备份当前配置 ----------
cp -f "$ENV_FILE" "$BACKUP"
log "已备份配置 → $BACKUP（回滚：./deploy.sh --rollback）"

# ---------- 3. 切换版本并重建 ----------
log "切换 XIAOZHI_TAG=$VERSION"
if grep -q '^XIAOZHI_TAG=' "$ENV_FILE"; then
  sed -i "s/^XIAOZHI_TAG=.*/XIAOZHI_TAG=$VERSION/" "$ENV_FILE"
else
  echo "XIAOZHI_TAG=$VERSION" >> "$ENV_FILE"
fi

log "拉取并重建容器（会短暂中断设备连接，设备会自动重连）"
$COMPOSE pull || fail "镜像拉取失败"
$COMPOSE up -d || fail "容器重建失败"

# ---------- 4. 数据库增量 ----------
# 约定：db/upgrade/ 下的脚本必须幂等（CREATE TABLE IF NOT EXISTS / INSERT IGNORE）
if [ -d db/upgrade ]; then
  log "执行数据库增量脚本"
  for f in $(ls -1 db/upgrade/*.sql 2>/dev/null | sort); do
    $COMPOSE exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi' < "$f" 2>/dev/null \
      && log "  ✓ $(basename "$f")" \
      || log "  ⚠️ $(basename "$f") 执行失败（可能已执行过，检查日志）"
  done
else
  log "没有 db/upgrade 目录，跳过数据库增量"
fi

# ---------- 5. 清权限缓存 ----------
log "清理权限缓存（否则新菜单/接口仍按旧权限判定）"
$COMPOSE exec -T redis redis-cli --scan --pattern 'XiaoZhi:Permission*' 2>/dev/null \
  | while read -r k; do $COMPOSE exec -T redis redis-cli del "$k" >/dev/null 2>&1; done
log "  ✓ 缓存已清理"

# ---------- 6. 健康检查 ----------
log "等待服务健康（最多 ${HEALTH_TIMEOUT}s）"
deadline=$(( $(date +%s) + HEALTH_TIMEOUT ))
while [ $(date +%s) -lt $deadline ]; do
  unhealthy=$($COMPOSE ps --format '{{.Name}} {{.Status}}' | grep -vE 'healthy|Exited' | grep -E 'start|unhealthy' || true)
  [ -z "$unhealthy" ] && break
  sleep 5
done

echo
$COMPOSE ps --format '{{.Name}} {{.Image}} {{.Status}}'

if $COMPOSE ps --format '{{.Status}}' | grep -q 'unhealthy'; then
  log "❌ 存在 unhealthy 容器，可用 ./deploy.sh --rollback 回滚到上一个版本"
  exit 1
fi

log "✅ 部署完成：$VERSION"
