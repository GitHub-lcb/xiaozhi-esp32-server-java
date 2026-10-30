#!/usr/bin/env bash
cd /opt/xiaozhi
MP=$(docker volume inspect xiaozhi_dialogue_logs -f '{{.Mountpoint}}')
echo "mountpoint: $MP"
ls -lt "$MP" | head -8
echo '=== 角色相关日志（全文件找）==='
grep -rlE '角色保存|roleUpdated|角色属性变更' "$MP" 2>/dev/null | head -5
grep -hE '角色保存|roleUpdated|角色属性变更' "$MP"/* 2>/dev/null | grep -E '2026-10-30 0[6-9]:' | head -20
echo '=== 08 时段 TTS 报错 ==='
grep -hE '2026-10-30 0[7-9]:' "$MP"/* 2>/dev/null | grep -iE 'TTS|voice|音色' | grep -iE 'error|warn|失败|void' | head -20
