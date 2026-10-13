#!/usr/bin/env bash
# MedModels 构建入口（Hermes 用；环境变量集中在此）
# JDK 策略（K-004）：不能用 Android Studio 的 JBR 25（版本串无法被 Gradle 内嵌
# Kotlin 解析）。需要 JDK 17+。
# 跨平台（2026-10-13 迁移 macOS 后重写）：
#   优先级：已设 JAVA_HOME → macOS brew openjdk@17 → macOS java_home -v 17
#           → NixOS 系统声明式 → NixOS 已知 store 路径兜底
set -eu

find_java17() {
  # 1) brew openjdk@17（keg-only，不注册 java_home）
  local brew17="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  [ -x "$brew17/bin/java" ] && { echo "$brew17"; return; }
  # 2) macOS java_home 指定 17+
  if [ -x /usr/libexec/java_home ]; then
    local jh
    jh=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
    [ -n "$jh" ] && [ -x "$jh/bin/java" ] && { echo "$jh"; return; }
  fi
  # 3) NixOS：系统 PATH 里的 java（声明式 openjdk17）
  if command -v java >/dev/null 2>&1; then
    local jbin jhome
    jbin=$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)
    jhome=${jbin%/bin/java}
    [ -x "$jhome/bin/java" ] && { echo "$jhome"; return; }
  fi
  # 4) NixOS：已知 store 路径兜底
  local nix17=/nix/store/kykaas4mrvyaw2365y2fg8sn22qh5qb0-openjdk-17.0.20+8/lib/openjdk
  [ -x "$nix17/bin/java" ] && { echo "$nix17"; return; }
  echo ""
}

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
  FOUND=$(find_java17)
  [ -n "$FOUND" ] && export JAVA_HOME="$FOUND"
fi
if [ -z "${JAVA_HOME:-}" ]; then
  echo "❌ 未找到 JDK 17+（macOS: brew install openjdk@17；NixOS: home.nix 声明）" >&2
  exit 1
fi
export PATH=$JAVA_HOME/bin:$PATH

# ANDROID_HOME：macOS 与 NixOS 路径不同（gradle 本身优先读 local.properties）
if [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
elif [ -d "$HOME/Android/Sdk" ]; then
  export ANDROID_HOME="$HOME/Android/Sdk"
fi

cd "$(dirname "$0")"
exec ./gradlew "$@"
