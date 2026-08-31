#!/bin/bash
# ./run.sh [-Dkey=value ...]   build if needed, then serve on port 8080
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d out ] || [ -n "$(find src -newer out -type f -print -quit)" ]; then
  ./build.sh
fi

exec java -cp "out:lib/*" "$@" com.acme.nl2sql.Nl2SqlApplication
