#!/usr/bin/env bash
cd /opt/xiaozhi
echo "=== dialogue 是否暴露 actuator ==="
docker compose exec -T dialogue sh -c 'curl -s -o /dev/null -w "prom=%{http_code} " http://127.0.0.1:8092/actuator/prometheus; curl -s -o /dev/null -w "health=%{http_code}\n" http://127.0.0.1:8092/actuator/health' 2>/dev/null || echo "dialogue 无 curl 或无 actuator"
echo "=== dialogue 是否有 tts 相关指标行 ==="
docker compose exec -T dialogue sh -c 'curl -s http://127.0.0.1:8092/actuator/prometheus' 2>/dev/null | grep -E "^# HELP xiaozhi_tts|^xiaozhi_tts" | head -5
echo "=== 日志里 TTS 首帧线索（最近 1 小时 sentence_start 与 start 的间隔）==="
docker compose logs --since 1h dialogue 2>&1 | grep -E "sendTtsMessage" | grep -E "state: (start|sentence_start)" | tail -6
