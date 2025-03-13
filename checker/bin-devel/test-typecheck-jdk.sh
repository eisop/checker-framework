#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SCRIPTDIR"/clone-related.sh

./checker/bin/javac -processor nullness \
  --patch-module java.base=../jdk/src/java.base/share/classes \
  -Xmaxerrs 5000 \
  -nowarn \
  "$(find ../jdk/src/java.base/share/classes/java/util -maxdepth 1 -name '*.java')" \
