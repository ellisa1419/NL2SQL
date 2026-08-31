#!/bin/bash
# ./build.sh [clean]   compile to out/
set -euo pipefail
cd "$(dirname "$0")"

if [ "${1:-}" = "clean" ]; then
  rm -rf out
fi

mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
javac --release 17 -Xlint:all -cp "lib/*" -d out @out/sources.txt
cp -R src/main/resources/. out/

echo "built $(wc -l < out/sources.txt | tr -d ' ') sources -> out/"
