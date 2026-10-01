#!/bin/bash

echo Entering checker/bin-devel/clone-related.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

DEBUG=0
# To enable debugging, uncomment the following line.
# DEBUG=1

if [ $DEBUG -eq 0 ]; then
  DEBUG_FLAG=
else
  DEBUG_FLAG=--debug
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

IS_CI="$("$SCRIPT_DIR"/is-ci.sh)"
export IS_CI
if [ -n "$IS_CI" ]; then
  # CircleCI fails, for the Daikon job only, if "-Dorg.gradle.daemon=false" is removed.
  export GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.daemon=false -Dorg.gradle.console=plain -Xmx4g"
fi

export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

echo "initial JAVA_HOME=${JAVA_HOME}"
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  # shellcheck disable=SC2230
  export JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}
fi
echo "JAVA_HOME=${JAVA_HOME}"

# Using `(cd "$CHECKERFRAMEWORK" && ./gradlew getGitScripts -q)` leads to infinite regress.
GIT_SCRIPTS="${SCRIPT_DIR}/.git-scripts"
if [ -d "$GIT_SCRIPTS" ]; then
  (cd "$GIT_SCRIPTS" && (git pull -q || true))
else
  (cd "${SCRIPT_DIR}" \
    && (git clone --depth=1 -q https://github.com/eisop-plume-lib/git-scripts.git .git-scripts \
      || (sleep 60 && git clone --depth=1 -q https://github.com/eisop-plume-lib/git-scripts.git .git-scripts)))
fi

# Clone the annotated JDK into ../jdk .
"$GIT_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop jdk

# Download Gradle and dependencies, retrying in case of network problems.
# Under CircleCI, the `timeout` command seems to hang forever.
if [ -z "$CIRCLECI" ]; then
  # echo "NO_WRITE_VERIFICATION_METADATA=$NO_WRITE_VERIFICATION_METADATA"
  if [ -z "${NO_WRITE_VERIFICATION_METADATA+x}" ]; then
    # Note that "timeout" is not compatible with shell functions.
    TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run --quiet \
      || { echo "./gradlew --write-verification-metadata sha256 help --dry-run failed; sleeping before trying again." \
        && sleep 1m \
        && echo "Trying again: ./gradlew --write-verification-metadata sha256 help --dry-run" \
        && TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run; }
  fi
fi

echo Exiting checker/bin-devel/clone-related.sh in "$(pwd)"
