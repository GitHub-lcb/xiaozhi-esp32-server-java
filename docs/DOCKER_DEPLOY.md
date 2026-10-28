# 云服务器 Docker 部署（国内网络）

[Docker 部署](./DOCKER.md) 讲的是通用流程。本文记录在国内云服务器上真机部署一遍的完整过程：
**只有 80/443 对外开放、访问不了 GitHub、2C4G 小内存**这类约束下，每一步怎么处理、会踩哪些坑。

实测环境：Ubuntu 24.04 / 2 vCPU / 3.4G 内存 / 阿里云。

## 0. 最短路径

```bash
# 1) 装 docker 与 compose 插件，配镜像加速（见第 1、2 节）
# 2) 拿代码
git clone https://github.com/<owner>/xiaozhi-esp32-server-java.git && cd xiaozhi-esp32-server-java

# 3) 按需改 .env：数据库口令、HOST_IP、对外域名
cp .env.example .env

# 4) 构建（首次 20~40 分钟，取决于网络和 CPU）
docker compose -f docker-compose.yml -f docker-compose.build.yml build

# 5) 起服务
docker compose up -d

# 6) 域名 + HTTPS：见第 8 节，站点配置见 deploy/nginx/xiaozhi.conf.example
```

## 1. Docker 与 compose 插件

```bash
apt-get update && apt-get install -y docker.io docker-compose-v2
docker --version && docker compose version
```

Ubuntu 打包的 compose 插件装在 `/usr/libexec/docker/cli-plugins/`，有些 docker CLI 版本扫不到这个目录，
表现为插件明明装了却报：

```
docker: unknown command: docker compose
```

把插件放到用户级目录即可：

```bash
mkdir -p /root/.docker/cli-plugins
cp /usr/libexec/docker/cli-plugins/docker-compose /root/.docker/cli-plugins/
chmod +x /root/.docker/cli-plugins/docker-compose
docker compose version
```

## 2. 镜像加速

国内机器直连 Docker Hub 基本会超时（`registry-1.docker.io` i/o timeout），需要配镜像源：

```bash
cat > /etc/docker/daemon.json <<'JSON'
{
  "registry-mirrors": ["https://docker.m.daocloud.io", "https://docker.1panel.live"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
JSON
systemctl restart docker
docker pull redis:7-alpine   # 验证
```

镜像源会失效，拉不动就换一个。探活：`curl -s -o /dev/null -w '%{http_code}' https://<mirror>/v2/`，
返回 200/401 都算通。

## 3. 代码

优先 `git clone`。服务器访问 GitHub 太慢或不通时，本地打包含再传：

```bash
# 本机
git archive --format=tar.gz -o repo.tar.gz --prefix=xiaozhi/ HEAD
scp repo.tar.gz root@<server>:/tmp/

# 服务器
mkdir -p /opt/xiaozhi && tar xzf /tmp/repo.tar.gz -C /opt/xiaozhi --strip-components=1
```

## 4. 数据库

项目**不使用 Flyway**，`db/init.sql` 是唯一的建表入口：建账号、建库、15 张表、基础数据一次做完。
`docker-compose.yml` 已把它挂进 MySQL 的初始化目录：

```yaml
volumes:
  - mysql_data:/var/lib/mysql
  - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
```

两个注意点：

- **只在 MySQL 数据目录为空时执行**。容器初始化过之后再改 `init.sql` 不会生效，
  要重新初始化得 `docker compose down -v`（会删数据）后重来。
- 用**非 123456 的口令**时，脚本里建的 `xiaozhi@localhost` 与 compose 用环境变量建的
  `xiaozhi@%` 口令会不一致。应用跨容器连 `mysql:3306`，走 `@%`，不受影响；
  `docker exec` 进容器手工连接要用 root。

验证：

```bash
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" xiaozhi-mysql-1 \
  mysql -uroot -e "select count(*) from information_schema.tables where table_schema='xiaozhi';"
# 预期 15
```

## 5. 模型与原生库

`Dockerfile-server` 在构建阶段从 GitHub 下载约 1.5GB 模型与原生库。国内服务器这一步大概率失败，
两种处理方式：

**A. 构建期走代理**：`.env` 里配 `BUILD_PROXY=http://<你的代理>`。

**B. 本机下好再挂载**（没有代理时用这个）：

```bash
# 本机（能访问 GitHub 的机器），按目标平台下载
TARGET_PLATFORM=linux-x64 ./xiaozhi.sh download base
TARGET_PLATFORM=linux-x64 ./xiaozhi.sh download stt sensevoice
TARGET_PLATFORM=linux-x64 ./xiaozhi.sh download tts vits-melo

# 传到服务器
rsync -avz models/ lib/ root@<server>:/opt/xiaozhi/
```

然后把 Dockerfile 里"下载模型和原生库"那段 RUN 去掉，并在 `docker-compose.override.yml` 里挂进去：

```yaml
services:
  server:
    volumes:
      - ./models:/app/models
      - ./lib:/app/lib
  dialogue:
    volumes:
      - ./models:/app/models
      - ./lib:/app/lib
```

只跑通链路、暂时不用本地语音时，什么都不挂也能起：VAD 模型随代码一起打包，
STT/TTS 缺模型只会退化成"未配置第三方 STT/TTS"，前端和接口都能正常用。

## 6. 构建

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml build
```

小内存机器（2C4G）会在 `mvn package` 阶段失败：

```
[ERROR] There was an error in the forked process
[ERROR] Java heap space
```

原因是 Dockerfile 不跳过单测，容器内跑全量测试（尤其 ArchUnit 那类要扫全量 class 的）时堆不够。
测试交给 CI，这里跳过：

```bash
sed -i 's|mvn -Dmaven.repo.local=/root/.m2/repository package|mvn -Dmaven.repo.local=/root/.m2/repository package -DskipTests|' Dockerfile-server
```

## 7. 启动与自检

```bash
docker compose up -d
docker compose ps --format '{{.Name}}  {{.Status}}'
```

正常应为 5 个容器全部 healthy（web 首次会先经历十几秒 health: starting）。

```bash
# 接口自检：返回 401 JSON 说明链路通（未登录）
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8084/api/

# 登录自检
curl -s -X POST http://127.0.0.1:8084/api/user/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

## 8. 域名与 HTTPS

云服务器安全组一般只放行 80/443，而 compose 暴露的是 web 8084、server 8091、dialogue 8092。
所以需要一层 nginx 反代，同时解决 HTTPS 与 WebSocket 升级。

**域名规则**写在 `ServerAddressProvider` 里，配了 `XIAOZHI_SERVER_DOMAIN=<domain>` 之后给设备下发：

| 用途 | 地址 | 需要的 DNS |
|---|---|---|
| OTA | `https://<domain>/api/device/ota` | `<domain>` |
| WebSocket | `wss://ws.<domain>/ws/xiaozhi/v1/` | `ws.<domain>` |
| MCP | `wss://mcp.<domain>/ws/mcp/` | `mcp.<domain>` |

不配域名则回落到 `http://<IP>:8091` + `ws://<IP>:8092`，需要安全组额外放行这两个端口。

站点配置直接用 [deploy/nginx/xiaozhi.conf.example](../deploy/nginx/xiaozhi.conf.example)，把 `<domain>` 换成你的域名：

```bash
cp deploy/nginx/xiaozhi.conf.example /etc/nginx/sites-available/xiaozhi
ln -sf /etc/nginx/sites-available/xiaozhi /etc/nginx/sites-enabled/xiaozhi
nginx -t && systemctl reload nginx
```

**证书**：每个域名一张（或一张多 SAN 证书）放到 `/etc/nginx/ssl/<域名>.pem` 与 `.key`，
pem 里要带中间证书（证书服务商 nginx 包里的 `_bundle.crt` 就是）。通配符不覆盖裸域：

- `*.example.com` 不包含 `example.com`
- `*.ai.example.com` 不包含 `ai.example.com`

反代之后还要让应用知道真实客户端 IP，否则限流与封禁会把所有请求算成同一个 IP：

```yaml
environment:
  XIAOZHI_SECURITY_TRUSTED_PROXIES: "127.0.0.1,172.16.0.0/12"
  XIAOZHI_SERVER_DOMAIN: "ai.example.com"
```

## 9. 验证清单

| 项 | 命令 | 预期 |
|---|---|---|
| 页面 | `curl -sI https://<domain>/` | 200 |
| 接口 | `curl -s -o /dev/null -w '%{http_code}' https://<domain>/api/` | 401（未登录） |
| 登录 | POST `/api/user/login` | 200 + token |
| OTA | POST `/api/device/ota` 带空 body | 400 `设备ID不正确`（说明端点通） |
| WebSocket | wss 连 `wss://ws.<domain>/ws/xiaozhi/v1/` | 401（缺设备 token 即正常） |
| 证书链 | `openssl s_client -connect <domain>:443 -servername <domain> -CAfile /etc/ssl/certs/ca-certificates.crt` | Verify return code: 0 |

## 10. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `docker: unknown command: docker compose` | CLI 没扫到插件目录 | 把 `docker-compose` 拷到 `~/.docker/cli-plugins/` |
| 拉镜像 `i/o timeout` | Docker Hub 不可达 | 配 `registry-mirrors` |
| 构建 `Java heap space` | 小内存机器跑单测 | 加 `-DskipTests` |
| web 一直 unhealthy | 健康检查写 localhost，容器内解析到 ::1 | 用 `127.0.0.1`（仓库已修正） |
| 打开页面 502 | `server_name` 没命中，或上游容器没起 | `nginx -T` 看实际加载的 server 块 |
| 设备连不上 | `ws.` 域名证书不匹配 / DNS 没解析 | 核对证书 SAN 与解析结果 |
| 登录 400 密码不正确 | 密码被改过，或库里是 `init.sql` 的默认值 | 用默认口令登录，必要时重置 |
| 改了 `db/init.sql` 不生效 | MySQL 只在数据目录为空时执行初始化 | `docker compose down -v` 后重来 |

## 11. 维护

```bash
# 升级：拉代码后重建镜像并滚动重启
git pull && docker compose -f docker-compose.yml -f docker-compose.build.yml build
docker compose up -d

# 看日志
docker compose logs -f server

# 证书续期：替换 /etc/nginx/ssl/ 下的 pem 与 key 后
nginx -t && systemctl reload nginx
```
