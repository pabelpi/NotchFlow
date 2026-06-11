#!/bin/sh
set -e

export JAVA_HOME=${JAVA_HOME_17_X64:-$JAVA_HOME}

exec ./gradlew.real "$@" 2>/dev/null || {
    if command -v gradle >/dev/null 2>&1; then
        gradle "$@"
    else
        echo "Gradle not found"
        exit 1
    fi
}
