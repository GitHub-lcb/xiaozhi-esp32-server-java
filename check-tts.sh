#!/usr/bin/env bash
cd /opt/xiaozhi
echo '=== TTS 错误日志（近 3 小时）==='
docker compose logs --since 3h dialogue 2>&1 | grep -iE 'TTS返回错误|TTS请求失败|TTS合成出错|55000000|未找到音频|resource' | tail -15
echo '=== 角色音色配置 ==='
M='docker compose exec -T mysql sh -c'
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT roleId, roleName, voiceName, ttsId, state FROM sys_role"' 2>/dev/null
$M 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaozhi -e "SELECT configId, configName, provider, state FROM sys_config WHERE configType=(select min(configType) from sys_config where configType like char(116,116,115))"' 2>/dev/null
