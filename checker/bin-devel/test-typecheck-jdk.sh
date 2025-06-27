#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SCRIPTDIR"/clone-related.sh

# Run assembleForJavac because it does not build the javadoc, so it is faster than assemble.
echo "running \"./gradlew assembleForJavac\" for checker-framework"
./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

./checker/bin/javac -processor nullness -AassumeInitialized -AassumeKeyFor -AprintVerboseGenerics -AassumeAssertionsAreEnabled \
  --patch-module java.base=../jdk/src/java.base/share/classes \
  -Xmaxerrs 5000 \
  -nowarn \
  -Alint=-all \
  -XDignore.symbol.file=true \
  ../jdk/src/java.base/share/classes/java/lang/*.java
