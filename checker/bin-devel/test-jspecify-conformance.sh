#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh

GIT_SCRIPTS="$SCRIPTDIR/.git-scripts"
"$GIT_SCRIPTS/git-clone-related" eisop jspecify-conformance
cd ../jspecify-conformance
./gradlew test --console=plain -PcfLocal
