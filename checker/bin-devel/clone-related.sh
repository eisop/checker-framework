#!/bin/bash

echo Entering checker/bin-devel/clone-related.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

DEBUG=0
# To enable debugging, uncomment the following line.
# DEBUG=1

if [ $DEBUG -eq 0 ] ; then
  DEBUG_FLAG=
else
  DEBUG_FLAG=--debug
fi

echo "initial CHECKERFRAMEWORK=$CHECKERFRAMEWORK"
export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

echo "initial JAVA_HOME=${JAVA_HOME}"
if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  # shellcheck disable=SC2230
  export JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}
fi
echo "JAVA_HOME=${JAVA_HOME}"

# Using `(cd "$CHECKERFRAMEWORK" && ./gradlew getPlumeScripts -q)` leads to infinite regress.
PLUME_SCRIPTS="$CHECKERFRAMEWORK/checker/bin-devel/.plume-scripts"
if [ -d "$PLUME_SCRIPTS" ] ; then
  (cd "$PLUME_SCRIPTS" && (git pull -q || true))
else
  (cd "$CHECKERFRAMEWORK/checker/bin-devel" && \
      (git clone --filter=blob:none -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts || \
       (sleep 1m && git clone --filter=blob:none -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts)))
fi

# Clone the annotated JDK into ../jdk .
"$PLUME_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop jdk

# AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
# # Don't use `AT=${AFU}/..` which causes a git failure.
# AT=$(dirname "${AFU}")

# ## Build annotation-tools (Annotation File Utilities)
# "$PLUME_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop annotation-tools "${AT}"
# if [ ! -d ../annotation-tools ] ; then
#   ln -s "${AT}" ../annotation-tools
# fi

# echo "Running:  (cd ${AT} && ./.build-without-test.sh)"
# (cd "${AT}" && ./.build-without-test.sh)
# echo "... done: (cd ${AT} && ./.build-without-test.sh)"


# Download dependencies, trying a second time if there is a failure.
(TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run || \
     (sleep 1m && ./gradlew --write-verification-metadata sha256 help --dry-run))

echo Exiting checker/bin-devel/clone-related.sh in "$(pwd)"
