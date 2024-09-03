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

# Using `(cd "$CHECKERFRAMEWORK" && ./gradlew getGitScripts -q)` leads to infinite regress.
GIT_SCRIPTS="$CHECKERFRAMEWORK/checker/bin-devel/.git-scripts"
if [ -d "$GIT_SCRIPTS" ] ; then
  (cd "$GIT_SCRIPTS" && (git pull -q || true))
else
  (cd "$CHECKERFRAMEWORK/checker/bin-devel" && \
      (git clone --depth=1 -q https://github.com/eisop-plume-lib/git-scripts.git .git-scripts || \
       (sleep 60 && git clone --depth=1 -q https://github.com/eisop-plume-lib/git-scripts.git .git-scripts)))
fi

# Clone the annotated JDK into ../jdk .
"$GIT_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop jdk

# AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
# # Don't use `AT=${AFU}/..` which causes a git failure.
# AT=$(dirname "${AFU}")

# ## Build annotation-tools (Annotation File Utilities)
# "$GIT_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop annotation-tools "${AT}"
# if [ ! -d ../annotation-tools ] ; then
#   ln -s "${AT}" ../annotation-tools
# fi

# echo "Running:  (cd ${AT} && ./.build-without-test.sh)"
# (cd "${AT}" && ./.build-without-test.sh)
# echo "... done: (cd ${AT} && ./.build-without-test.sh)"


# Download dependencies, trying a second time if there is a failure.
(TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run --quiet || \
     (echo "./gradlew --write-verification-metadata sha256 help --dry-run --quiet failed; sleeping before trying again." && \
      sleep 1m && \
      echo "Trying again: ./gradlew --write-verification-metadata sha256 help --dry-run --quiet" && \
      TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run --quiet))

echo Exiting checker/bin-devel/clone-related.sh in "$(pwd)"
