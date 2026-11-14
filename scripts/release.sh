#!/bin/sh
# Gera o APK assinado e publica como GitHub Release (o link "latest" sempre aponta para ele).
set -eu
cd "$(dirname "$0")/.."
VER=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
TAG="v$VER"
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
    echo "A tag $TAG já existe. Aumente versionCode e versionName em app/build.gradle.kts." >&2
    exit 1
fi
./g :app:assembleRelease
mkdir -p build/dist
cp app/build/outputs/apk/release/app-release.apk build/dist/uno-launcher.apk
git tag "$TAG"
git push origin main --tags
gh release create "$TAG" build/dist/uno-launcher.apk --repo dudubarbaro/uno-launcher \
    --title "Uno Launcher $VER" --notes "APK para a central e para o tablet."
echo "Publicado: https://github.com/dudubarbaro/uno-launcher/releases/latest/download/uno-launcher.apk"
