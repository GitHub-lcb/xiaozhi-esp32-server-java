# 部署与发布

> 面向维护者：服务器上怎么起、新版本怎么发、出问题怎么回滚。

## 一、首次部署（服务器）

```bash
# 1. 准备目录与配置
mkdir -p /opt/xiaozhi/music && cd /opt/xiaozhi
curl -fsSL -o docker-compose.yml \
  "https://api.github.com/repos/GitHub-lcb/xiaozhi-esp32-server-java/contents/docker-compose.yml?ref=main" \
  -H "Accept: application/vnd.github.raw"
curl -fsSL -o .env \
  "https://api.github.com/repos/GitHub-lcb/xiaozhi-esp32-server-java/contents/.env.example?ref=main" \
  -H "Accept: application/vnd.github.raw"

# 2. 必填项（.env）
#    XIAOZHI_TAG=<版本号>                镜像版本，如 6.2.11
#    HOST_IP=<服务器内网IP>              Docker Desktop 必须填，Linux 可留空自动探测
#    XIAOZHI_DEVICE_AUTH_SECRET=<随机串> 设备接入鉴权
#    MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD
#    XIAOZHI_OPS_TOKEN=<随机串>          配了才能免登录抓 /actuator/prometheus

# 3. 起服务（首次会自动执行 db/init.sql 建表）
docker compose up -d
docker compose ps
```

首次启动后访问 `http://<服务器IP>:8084`，默认管理员 `admin / 123456`（登录后立即改密码）。

## 二、发布新版本

```bash
cd /opt/xiaozhi

# ① 在本地打 tag 并推送，触发 CI 构建镜像
#    git tag -a v6.2.11 -m "..." && git push origin v6.2.11

# ② 等 Actions → docker 流水线转绿
#    https://github.com/GitHub-lcb/xiaozhi-esp32-server-java/actions

# ③ 服务器上执行（自动备份、切版本、跑增量、清缓存、健康检查）
./deploy.sh 6.2.11

# 只想确认镜像是否就绪，不实际变更：
./deploy.sh 6.2.11 --dry-run
```

`deploy.sh` 做的事，按顺序：

| 步骤 | 动作 | 失败会怎样 |
|---|---|---|
| 1 | 拉取 server/dialogue/web **三个**镜像，缺一个就中止 | 不会出现"只切换了一半" |
| 2 | 备份 `.env` 到 `.env.backup-<时间戳>` | 可回滚 |
| 3 | 改 `XIAOZHI_TAG`，`compose pull && up -d` | 失败即退出 |
| 4 | 执行 `db/upgrade/*.sql`（必须幂等） | 单个脚本失败只告警，继续 |
| 5 | 清 `XiaoZhi:Permission*` 缓存 | —— |
| 6 | 等容器 healthy（最多 180s） | 超时提示回滚 |

## 三、回滚

```bash
cd /opt/xiaozhi
./deploy.sh --rollback          # 恢复最近一次 .env 备份并重建容器
```

数据库不回滚：升级脚本一律用 `CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE` / 加列不删列，
所以旧版本代码在新表结构上仍可运行。真要回退数据，用发布前的 `mysqldump` 备份。

## 四、数据库增量约定

- 所有结构变更写两份：
  1. `db/init.sql`（全新安装）
  2. `db/upgrade/<日期>__<说明>.sql`（已有库升级）
- 升级脚本**必须幂等**，可重复执行
- `deploy.sh` 会按文件名顺序执行 `db/upgrade/` 下所有脚本，不记录执行历史（靠幂等性兜底）

## 五、常用运维命令

```bash
docker compose ps                                   # 容器状态与版本
docker compose logs -f --tail=200 dialogue          # 对话服务日志（TTS/STT 都在这里）
docker compose logs --since 10m server | grep ERROR # 后台接口报错
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi'
docker compose exec redis redis-cli --scan --pattern 'XiaoZhi:Permission*'   # 权限缓存
```

## 六、功能开关（环境变量，改完必须 `up -d` 重建才生效）

| 变量 | 默认 | 说明 |
|---|---|---|
| `XIAOZHI_MQTT_ENABLED` | false | MQTT 控制面（唤醒/推送/远程指令）。**设备固件若不支持 MQTT，务必保持 false**，否则 OTA 会下发 mqtt 配置导致设备连不上 |
| `XIAOZHI_OPS_TOKEN` | 空 | Prometheus 免登录令牌 |
| `XIAOZHI_MEMORY_AUTO_EXTRACT` | false | 会话结束自动提炼长期记忆 |
| `XIAOZHI_MCP_ALLOW_WRITE` | false | MCP 接入点是否允许写操作 |
| `XIAOZHI_DEVICE_AUTH_SECRET` | 空 | 设备接入鉴权密钥，空=不设防 |

## 七、曲库

歌曲放在宿主机 `/opt/xiaozhi/music`（compose 挂载到容器 `/app/uploads/music`）：

- 传歌：`scp 歌名.mp3 root@<server>:/opt/xiaozhi/music/`
- 或后台「智能体 → 曲库」页面上传
- 传完即生效，无需重启；新文件会在下次打开页面时自动登记到 `sys_music`
