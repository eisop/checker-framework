#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
source "$SCRIPTDIR"/build.sh

# TODO: Maybe I should move this into the CI job, and do it for all CI jobs.
cp "$SCRIPTDIR"/mvn-settings.xml ~/settings.xml

"$SCRIPTDIR/.plume-scripts/git-clone-related" eisop guava
cd ../guava

if [ "$TRAVIS" = "true" ] ; then
  # There are two reasons that this script does not work on Travis.
  # 1. Travis kills jobs that do not produce output for 10 minutes.  (This can be worked around.)
  # 2. Travis kills jobs that produce too much output.  (This cannot be worked around.)
  echo "On Travis, use scripts that run just one type-checker."
  exit 1
fi

## This command works locally, but on Azure it fails with timouts while downloading Maven dependencies.
# cd guava && time mvn --debug -B package -P checkerframework-local -Dmaven.test.skip=true -Danimal.sniffer.skip=true

# The maven.wagon settings should not be relevant to Maven 3.9 and later, but try them anyway.
cd guava && time mvn --debug -B compile -P checkerframework-local \
  -Dhttp.keepAlive=false -Daether.connector.http.connectionMaxTtl=25 -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
