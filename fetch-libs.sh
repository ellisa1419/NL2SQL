#!/bin/bash
# ./fetch-libs.sh   re-download the runtime jars from Maven Central into lib/
set -euo pipefail
cd "$(dirname "$0")"

REPO=https://repo.maven.apache.org/maven2
ARTIFACTS=(
  "org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar"
  "com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar"
  "com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar"
  "com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar"
  "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar"
  "ch/qos/logback/logback-classic/1.5.11/logback-classic-1.5.11.jar"
  "ch/qos/logback/logback-core/1.5.11/logback-core-1.5.11.jar"
)

mkdir -p lib
for a in "${ARTIFACTS[@]}"; do
  out="lib/$(basename "$a")"
  if [ -f "$out" ]; then
    echo "have  $(basename "$a")"
    continue
  fi
  echo "fetch $(basename "$a")"
  curl -fsSL "$REPO/$a" -o "$out"
done
