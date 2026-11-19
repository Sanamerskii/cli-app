#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -d build src/*.java
if [ "${1:-}" = "--test" ]; then
    javac -cp build -d build tests/*.java
    java -cp build ConfigTest
else
    java -cp build Main "$@"
fi
