#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# Publish this checkout to the local Maven repository, so templatefora-checker can resolve it
# by coordinate.  A checker built on the Checker Framework consumes it as Maven artifacts, the
# way any other client does, so testing it that way exercises the published POMs: a missing or
# mis-scoped dependency fails as a resolution error naming the coordinate.  Javadoc is skipped:
# nothing here consumes it, and it is slow.
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

CF_VERSION="$(./gradlew -q :checker:properties | sed -n 's/^version: //p')"
if [ -z "${CF_VERSION}" ]; then
  echo "Could not determine the Checker Framework version; aborting." >&2
  exit 1
fi
echo "Testing templatefora-checker against io.github.eisop:*:${CF_VERSION}"

"$SCRIPT_DIR/.git-scripts/git-clone-related" eisop templatefora-checker

cd ../templatefora-checker

# -PcfVersion makes templatefora-checker resolve io.github.eisop artifacts at this checkout's
# version; its repositories consult mavenLocal for that group when the property is set, so the
# artifacts published above are the ones used.
./gradlew build --console=plain --warning-mode=all -PcfVersion="${CF_VERSION}" \
  --no-configuration-cache \
  -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
