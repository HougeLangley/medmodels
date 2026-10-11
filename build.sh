#!/usr/bin/env bash
# MedModels 构建入口（Hermes 用；环境变量集中在此）
# JDK 策略（K-004）：不能用 Android Studio 的 JBR 25（版本串无法被 Gradle 内嵌
# Kotlin 解析）。优先用系统声明式安装的 openjdk17（home.nix），兜底已知 store 路径。
if command -v java >/dev/null 2>&1; then
  JBIN=$(readlink -f "$(command -v java)")          # 解析 symlink 到真实路径
  JHOME=${JBIN%/bin/java}
  [ -x "$JHOME/bin/java" ] && export JAVA_HOME="$JHOME"
fi
if [ -z "${JAVA_HOME:-}" ]; then
  export JAVA_HOME=/nix/store/kykaas4mrvyaw2365y2fg8sn22qh5qb0-openjdk-17.0.20+8/lib/openjdk
fi
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Android/Sdk
cd "$(dirname "$0")"
exec ./gradlew "$@"

