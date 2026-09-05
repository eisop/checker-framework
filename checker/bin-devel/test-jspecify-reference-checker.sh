#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

# TODO: remove uses of `main-eisop` once that becomes `main`.
"$SCRIPT_DIR/.git-scripts/git-clone-related" --upstream-branch main-eisop eisop jspecify-reference-checker
"$SCRIPT_DIR/.git-scripts/git-clone-related" jspecify jspecify

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

# Clone JSpecify's own annotated-JDK fork to a directory separate from '../jdk' (which stays as
# this project's own eisop/jdk clone -- framework:copyAndMinimizeAnnotatedJdkFiles always needs
# it, even when this composite build is only assembling jspecify-reference-checker). Pass the new
# directory as jspecifyJdkHome so jspecify-reference-checker's own build looks there instead of
# its default '../jdk' (requires a matching override in jspecify-reference-checker's build.gradle).
"$SCRIPT_DIR/.git-scripts/git-clone-related" jspecify jdk ../jdk-jspecify

JSPECIFY_CONFORMANCE_TEST_MODE=details ./gradlew build conformanceTests demoTest --console=plain --include-build "$CHECKERFRAMEWORK" --no-configuration-cache -PjspecifyJdkHome="$CHECKERFRAMEWORK/../jdk-jspecify"
