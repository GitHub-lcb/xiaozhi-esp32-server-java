#!/usr/bin/env bash
MP=/var/lib/docker/volumes/xiaozhi_dialogue_logs/_data
echo '=== 08:36:15 之后 90 秒内的关键日志 ==='
awk '/2026-10-30 08:36:1[5-9]/,/2026-10-30 08:37:5/' "$MP/xiaozhi-dialogue.log" | grep -iE 'persona|tts|voice|合成|会话|session' | head -25
echo '=== 08:30-09:10 Persona 构建 / TTS 使用 ==='
grep -hE '2026-10-30 0[89]:' "$MP/xiaozhi-dialogue.log" | grep -iE 'Persona|音色|voice|TTS 合成|synthesize' | head -20
