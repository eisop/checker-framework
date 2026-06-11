#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

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
# This does not use "-PcfVersion=local", because that project does not
# use the CF gradle plugin.
./gradlew test --console=plain --warning-mode=all -PcfLocal
