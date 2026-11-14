#!/bin/sh
export JAVA_HOME="$HOME/tools/jdk-17" ANDROID_HOME="$HOME/tools/android-sdk"
exec "$HOME/tools/gradle-8.9/bin/gradle" -p "$(dirname "$0")" "$@"
