#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

# Publish this checkout to the local Maven repository, so the conformance project below can
# resolve it by coordinate. It previously read jars straight out of this checkout's build
# directories, which depended on framework-test/build/libs holding exactly one jar as a side
# effect of an unrelated task, and reported a missing one as "package
# org.checkerframework.framework.test does not exist" rather than as a missing dependency.
# Javadoc is skipped: nothing here consumes it, and it is slow.
./gradlew publishToMavenLocal -x javadoc -x allJavadoc --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

CF_VERSION="$(./gradlew -q :checker:properties | sed -n 's/^version: //p')"
if [ -z "${CF_VERSION}" ]; then
  echo "Could not determine the Checker Framework version; aborting." >&2
  exit 1
fi
echo "Testing jspecify-conformance against io.github.eisop:*:${CF_VERSION}"

"$SCRIPT_DIR/.git-scripts/git-clone-related" eisop jspecify-conformance
"$SCRIPT_DIR/.git-scripts/git-clone-related" jspecify jspecify
"$SCRIPT_DIR/.git-scripts/git-clone-related" jspecify jspecify-reference-checker

# Build conformance test artifacts locally.
# This duplicates logic from jspecify-conformance/.github/workflows/workflow.yml

trap 'rm -f /tmp/publish-helper.gradle' EXIT
cat > /tmp/publish-helper.gradle << 'INIT'
allprojects {
  pluginManager.apply('maven-publish')
  tasks.withType(Sign).configureEach { enabled = false }
}
INIT

cd ../jspecify
./gradlew --console=plain --warning-mode=all --init-script /tmp/publish-helper.gradle :conformance-tests:publishToMavenLocal

cd ../jspecify-reference-checker
cat > conformance-test-framework/settings.gradle << 'SETTINGS'
rootProject.name = 'conformance-test-framework'
dependencyResolutionManagement {
  versionCatalogs {
    libs {
      library('guava', 'com.google.guava:guava:33.6.0-jre')
      library('jspecify', 'org.jspecify:jspecify:1.0.0')
      library('truth', 'com.google.truth:truth:1.4.5')
      library('junit', 'junit:junit:4.13.2')
    }
  }
}
SETTINGS
./gradlew --project-dir conformance-test-framework \
  --console=plain --warning-mode=all --init-script /tmp/publish-helper.gradle publishToMavenLocal

cd ../jspecify-conformance
# -PcfVersion makes the project resolve io.github.eisop artifacts at this checkout's version;
# its repositories list mavenLocal() first, so the artifacts published above are the ones used.
./gradlew test --console=plain --warning-mode=all -PcfVersion="${CF_VERSION}"
