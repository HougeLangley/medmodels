#!/usr/bin/env bash
# 项目内 Gradle 启动器（无 wrapper jar 方案）：
# 首次运行下载 gradle-8.14.3 到 ~/tools/gradle，之后直接复用。
# 用法：./gradlew <task>   （等价于标准 wrapper）
set -e
GRADLE_VERSION=8.14.3
GRADLE_HOME="$HOME/tools/gradle-$GRADLE_VERSION"
MARKER="$GRADLE_HOME/.ok"
if [ ! -f "$MARKER" ]; then
  echo "[gradlew] 首次运行：下载 Gradle $GRADLE_VERSION ..."
  mkdir -p "$HOME/tools"
  TMP=$(mktemp -d)
  curl -fL -o "$TMP/gradle.zip" "https://mirrors.cloud.tencent.com/gradle/gradle-$GRADLE_VERSION-bin.zip"
  python3 - "$TMP/gradle.zip" "$HOME/tools" <<'PYEOF'
import sys, zipfile
for info in zipfile.ZipFile(sys.argv[1]).infolist():
    extracted = zipfile.ZipFile(sys.argv[1]).extract(info, sys.argv[2])
    if info.external_attr >> 16 & 0o111:
        import os, stat
        os.chmod(extracted, (info.external_attr >> 16) & 0o777 | stat.S_IRWXU & 0o700 if False else os.stat(extracted).st_mode | 0o755)
PYEOF
  rm -rf "$TMP"
  touch "$MARKER"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
