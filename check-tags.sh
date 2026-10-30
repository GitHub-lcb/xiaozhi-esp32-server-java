#!/usr/bin/env bash
for image in xiaozhi-server xiaozhi-dialogue xiaozhi-web; do
  TOKEN=$(curl -s "https://ghcr.io/token?scope=repository:github-lcb/$image:pull&service=ghcr.io" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  TAGS=$(curl -s -H "Authorization: Bearer $TOKEN" "https://ghcr.io/v2/github-lcb/$image/tags/list" | tr ',' '\n' | grep -oE '"(6\.2\.[0-9]+|latest)"' | tr -d '"' | sort -u | tr '\n' ' ')
  echo "$image: $TAGS"
done
echo '--- deploy log ---'
tail -4 /tmp/deploy-624.out
