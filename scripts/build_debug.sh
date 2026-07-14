#!/usr/bin/env bash
# 打包时自动递增版本号并构建 debug APK。
# 规则：versionCode +1；versionName 小版本 +1（1.1 -> 1.2 -> 1.3 ...）
set -euo pipefail

cd "$(dirname "$0")/.."   # OTPBox 项目根
export ANDROID_HOME="${ANDROID_HOME:-/usr/local/share/android-sdk}"

BUILD_FILE="app/build.gradle.kts"

if [[ ! -f "$BUILD_FILE" ]]; then
  echo "找不到 $BUILD_FILE" >&2
  exit 1
fi

# 提取当前版本
CURRENT_CODE=$(grep -E '^\s*versionCode = ' "$BUILD_FILE" | grep -oE '[0-9]+' | head -1)
CURRENT_NAME=$(grep -E '^\s*versionName = ' "$BUILD_FILE" | grep -oE '"[^"]+"' | head -1 | tr -d '"')

if [[ -z "$CURRENT_CODE" || -z "$CURRENT_NAME" ]]; then
  echo "无法解析当前版本号" >&2
  exit 1
fi

NEW_CODE=$((CURRENT_CODE + 1))

# 小版本 +1：1.2 -> 1.3 ; 1.10 -> 1.11
IFS='.' read -r MAJOR MINOR <<< "$CURRENT_NAME"
MINOR=$((10#$MINOR + 1))
NEW_NAME="$MAJOR.$MINOR"

echo "版本号: $CURRENT_NAME ($CURRENT_CODE)  ->  $NEW_NAME ($NEW_CODE)"

# 原地替换（仅替换版本号两行，避免误伤）
perl -i -pe "s/^(\s*versionCode = ).*/\$1$NEW_CODE/; s/^(\s*versionName = ).*/\$1\"$NEW_NAME\"/" "$BUILD_FILE"

echo "已更新 $BUILD_FILE"
echo "构建中..."
./gradlew :app:assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/koulinghezi-v${NEW_NAME}-debug.apk"
if [[ -f "$APK" ]]; then
  echo "APK: $APK"
else
  echo "未找到产物: $APK" >&2
  exit 1
fi
