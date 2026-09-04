#!/bin/bash

# Regression test for https://github.com/eisop/checker-framework/pull/1822:
# publish the checker-qual, checker-util, and checker artifacts the way a
# release would, then run a separate, standalone Gradle build that depends
# on the published `checker` artifact the way an external consumer would.
#
# This specifically guards against two failure modes that are invisible to
# the in-repo test suite, because that suite never resolves the published
# artifacts through Gradle Module Metadata the way an external project does:
#   1. The published org.gradle.jvm.version attribute not matching the
#      JDK the project actually targets (it previously reflected whatever
#      JDK ran Gradle on the publishing machine).
#   2. The `checker` artifact's published component lacking a compile-time
#      (java-api) variant, so `compileClasspath` resolution fails with
#      "No matching variant" even when the JVM version matches.
#   3. A published POM that omits a dependency the artifact needs at runtime.
#      The in-repo test suite cannot see this, because it never runs off the
#      published artifacts; the failure appears only when a consumer resolves
#      the POM, or when a bundled jar is missing classes it needs at load time.
#
# Publishing goes to an isolated, throwaway Maven-local repository (a fresh
# $HOME, so `~/.m2/repository` resolves underneath it) rather than the
# developer's real ~/.m2, so this is safe to run repeatedly and leaves no
# residue. The real $GRADLE_USER_HOME is kept so the existing Gradle
# dependency/build cache is still reused.

set -e
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../../.." &> /dev/null && pwd)"
CONSUMER_DIR="${SCRIPT_DIR}"

# Isolate `~/.m2/repository` for this run without disturbing the real
# Gradle cache: override $HOME (which is what `mavenLocal()` keys off of),
# but keep the real $GRADLE_USER_HOME so dependency/build caches are reused.
SMOKETEST_HOME="$(mktemp -d)"
trap 'rm -rf "${SMOKETEST_HOME}"' EXIT
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"

cd "${REPO_ROOT}"

CHECKER_VERSION="$(HOME="${SMOKETEST_HOME}" ./gradlew -q :checker:properties | sed -n 's/^version: //p')"
if [ -z "${CHECKER_VERSION}" ]; then
  echo "Could not determine the checker project version; aborting." >&2
  exit 1
fi
echo "Publishing and consuming version ${CHECKER_VERSION}"

# Publish exactly the artifacts an external consumer resolves: checker itself,
# plus checker-qual and checker-util, which are declared as regular (non-bundled)
# dependencies of the published `checker` artifact. The trailing `publishToMavenLocal`
# covers every other subproject, which is where framework, framework-all and their
# own dependencies (javacutil, dataflow) come from.
HOME="${SMOKETEST_HOME}" ./gradlew --console=plain \
  -x javadoc -x allJavadoc \
  :checker-qual:publishToMavenLocal \
  :checker-util:publishToMavenLocal \
  :checker:publishToMavenLocal \
  publishToMavenLocal

# Run the standalone consumer build against the freshly published artifacts.
# It has its own settings.gradle/build.gradle and is not part of this
# project's Gradle build, matching how an external consumer would see it.
cd "${CONSUMER_DIR}"
HOME="${SMOKETEST_HOME}" "${REPO_ROOT}/gradlew" --console=plain \
  -PcheckerVersion="${CHECKER_VERSION}" \
  clean smoketest

echo "Publish smoke test passed: a Java 8 consumer build resolved every published"
echo "io.github.eisop artifact at version ${CHECKER_VERSION}, compiled against checker,"
echo "and ran a checker out of the published framework-all artifact."
