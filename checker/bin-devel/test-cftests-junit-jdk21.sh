#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdkCompiler=21
source "$SCRIPTDIR"/clone-related.sh

# Adding --max-workers=1 to avoid random failures in Github Actions. An alternative solution is to use --no-build-cache.
# https://github.com/eisop/checker-framework/issues/849
./gradlew test -x javadoc -x allJavadoc --console=plain --warning-mode=all --max-workers=1
