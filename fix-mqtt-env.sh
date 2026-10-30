#!/usr/bin/env bash
cd /opt/xiaozhi
grep -q '^XIAOZHI_MQTT_BROKER_URL=' .env || echo 'XIAOZHI_MQTT_BROKER_URL=tcp://emqx:1883' >> .env
sed -i 's|^XIAOZHI_MQTT_BROKER_URL=.*|XIAOZHI_MQTT_BROKER_URL=tcp://emqx:1883|' .env
sed -i '/^XIAOZHI_MQTT_BROKER=/d' .env
HOSTIP=$(grep -E '^HOST_IP=.' .env | cut -d= -f2 || true)
grep -q '^XIAOZHI_MQTT_PUBLIC_ENDPOINT=' .env || echo "XIAOZHI_MQTT_PUBLIC_ENDPOINT=mqtt://${HOSTIP}:1883" >> .env
sed -i "s|^XIAOZHI_MQTT_PUBLIC_ENDPOINT=.*|XIAOZHI_MQTT_PUBLIC_ENDPOINT=mqtt://${HOSTIP}:1883|" .env
grep '^XIAOZHI_MQTT' .env
docker compose up -d dialogue 2>&1 | tail -2
sleep 50
docker compose logs --since 2m dialogue 2>&1 | grep -iE 'mqtt' | tail -4
docker compose exec -T emqx /opt/emqx/bin/emqx ctl clients list 2>&1 | head -3
