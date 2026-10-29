#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== 部署状态 ==='
grep '^XIAOZHI_TAG=' .env; docker compose ps --format '{{.Name}} {{.Image}} {{.Status}}'
echo '=== 角色变更/Persona 清理 ==='
docker compose logs --since 4h dialogue 2>&1 | grep -E '角色属性变更|roleUpdated|清理' | tail -5
echo '=== TTS/对话异常 ==='
docker compose logs --since 4h dialogue 2>&1 | grep -E 'TTS|ERROR|Exception' | grep -vE 'DEBUG|sql' | tail -12
