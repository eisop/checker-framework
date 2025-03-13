#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SCRIPTDIR"/clone-related.sh

PLUME_SCRIPTS="$SCRIPTDIR/.plume-scripts"

## Checker Framework demos
"$GIT_SCRIPTS/git-clone-related" eisop checker-framework.demos
./gradlew :checker:demosTests --console=plain --warning-mode=all

## Checker Framework templatefora-checker
"$GIT_SCRIPTS/git-clone-related" eisop templatefora-checker
./gradlew :checker:templateTests --console=plain --warning-mode=all

status=0

## Code style and formatting
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
if [ "${JAVA_VER}" != "8" ] ; then
  ./gradlew spotlessCheck --console=plain --warning-mode=all
fi
if grep -n -r --exclude-dir=build --exclude-dir=examples --exclude-dir=jtreg --exclude-dir=tests --exclude="*.astub" --exclude="*.tex" '^\(import static \|import .*\*;$\)'; then
  echo "Don't use static import or wildcard import"
  exit 1
fi
make -C checker/bin --jobs="$(getconf _NPROCESSORS_ONLN)"
make -C checker/bin-devel --jobs="$(getconf _NPROCESSORS_ONLN)"
make -C docs/developer/release check-python-style --jobs="$(getconf _NPROCESSORS_ONLN)"

set +o xtrace  # Turn off xtrace to avoid verbose output
# Check the definition of qualifiers in Checker Framework against the JDK
./checker/bin-devel/test-qualifier-consistency.sh
set -o xtrace  # Turn on xtrace output
