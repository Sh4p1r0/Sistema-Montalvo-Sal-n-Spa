#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=== Compilando fuentes Java ==="
mkdir -p build/classes
javac -d build/classes -cp "lib/postgresql-42.7.3.jar" $(find Proyecto_Diseño_Patrones/src -name "*.java")

echo "=== Iniciando Sistema Montalvo Salón & Spa ==="
java -Djava.awt.headless=true -cp "build/classes:lib/postgresql-42.7.3.jar" proyectodp.main.MainWeb
