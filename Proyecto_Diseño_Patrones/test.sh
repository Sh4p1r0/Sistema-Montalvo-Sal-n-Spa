#!/usr/bin/env bash
# Ejecuta la suite de pruebas (JUnit 5, sin instalar Maven).
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

JUNIT_VERSION="1.10.2"
JUNIT_DIR=".test-lib"
JUNIT_JAR="$JUNIT_DIR/junit-platform-console-standalone-${JUNIT_VERSION}.jar"
PG_JAR="lib/postgresql-42.7.3.jar"

if [ ! -f "$JUNIT_JAR" ]; then
  echo "=== Descargando JUnit 5 (una sola vez) ==="
  mkdir -p "$JUNIT_DIR"
  curl -fsSL -o "$JUNIT_JAR" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"
fi

echo "=== Compilando fuentes ==="
mkdir -p build/classes build/test-classes
javac -d build/classes -cp "$PG_JAR" $(find src -name "*.java")

echo "=== Compilando pruebas ==="
javac -d build/test-classes -cp "build/classes:$PG_JAR:$JUNIT_JAR" $(find test -name "*.java")

echo "=== Ejecutando pruebas ==="
java -jar "$JUNIT_JAR" execute \
  --class-path "build/classes:$PG_JAR:build/test-classes" \
  --scan-class-path build/test-classes \
  --details=tree \
  --fail-if-no-tests
