#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"


if type -p java; then
  _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  _java="$JAVA_HOME/bin/java"
else
  echo "Can't find java"
  exit 1
fi
version=$("$_java" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

if [[ "$version" -ge 9 ]]; then
  SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
  # shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
  source "$SCRIPTDIR"/build.sh

  ## Build JSpecify and use its tests.
  "$SCRIPTDIR/.plume-scripts/git-clone-related" jspecify jspecify

  # TODO: only build here, not in build.sh
  # echo "Running:  (cd ../jspecify/ && ./gradlew assemble)"
  # If failure, retry in case the failure was due to network lossage.
  # (cd ../jspecify/ && export JDK_JAVA_OPTIONS='--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED' && (./gradlew assemble || (sleep 60 && ./gradlew assemble)))
  # echo "... done: (cd ../jspecify/ && ./gradlew assemble)"

  # TODO: run tests
fi
