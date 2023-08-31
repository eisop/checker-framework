#!/bin/bash

echo Entering checker/bin-devel/build.sh in "$(pwd)"

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
  (cd "$PLUME_SCRIPTS" && git pull -q)
else
  (cd "$CHECKERFRAMEWORK/checker/bin-devel" && \
      (git clone --depth 1 -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts || \
       git clone --depth 1 -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts))
fi

# Clone the annotated JDK into ../jdk .
"$PLUME_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop jdk

# NO-AFU
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


## Build stubparser
"$PLUME_SCRIPTS/git-clone-related" ${DEBUG_FLAG} eisop stubparser
echo "Running:  (cd ../stubparser/ && ./.build-without-test.sh)"
(cd ../stubparser/ && ./.build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.build-without-test.sh)"

# TODO: NullnessNullMarkedTest depends on JSpecify annotations.
# Find a way to not run that test, to avoid this dependency and
# instead only use ./test-jspecify.sh.
## Build JSpecify, only for the purpose of using its tests.
"$PLUME_SCRIPTS/git-clone-related" jspecify jspecify
if type -p java; then
  _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  _java="$JAVA_HOME/bin/java"
else
  echo "Can't find java"
  exit 1
fi
version=$("$_java" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
if [[ "$version" -ge 9 ]]; then
  echo "Running:  (cd ../jspecify/ && ./gradlew assemble)"
  # If failure, retry in case the failure was due to network lossage.
  (cd ../jspecify/ && \
    # Temporarily, until a gradle 8.1 release is used, to allow JDK 20 tests to pass.
    sed -i "s/gradle-8.0-bin/gradle-8.1-rc-1-bin/" gradle/wrapper/gradle-wrapper.properties && \
    export JDK_JAVA_OPTIONS='--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED' && \
    (./gradlew assemble || (sleep 60 && ./gradlew assemble)))
  echo "... done: (cd ../jspecify/ && ./gradlew assemble)"
fi


## Compile

# Download dependencies, trying a second time if there is a failure.
(./gradlew --write-verification-metadata sha256 help --dry-run ||
     (sleep 60 && ./gradlew --write-verification-metadata sha256 help --dry-run))

echo "running \"./gradlew assembleForJavac\" for checker-framework"
./gradlew assembleForJavac --console=plain --warning-mode=all -s -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
