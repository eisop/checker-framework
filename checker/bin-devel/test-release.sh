#!/bin/bash

# Runs the release scripts as a dry run, without credentials, so that a problem in them, or in the
# build of the release, is found by a pull request rather than during a release.  See
# docs/developer/release/README-release-process.html.
#
# release_build.py --dry-run builds, commits, tags, and checks the release in a temporary release
# directory, from a clone of this checkout, signing with a throw-away key; release_push.py --dry-run
# runs the sanity checks, generates the web site from the zip file and checks its links, and checks
# the Maven artifacts, without pushing or publishing anything.  This script then checks what the
# scripts do not: the contents of the zip files, and the published Annotation File Utilities.
# The zip files are left in build/, for the nightly build.

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
ROOT_DIR="$(cd -- "$SCRIPT_DIR/../.." &> /dev/null && pwd)"
cd "$ROOT_DIR"

fail() {
  set +x
  echo "test-release.sh: $*" >&2
  exit 1
}

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT


# The version of the release, which is releaseVersion in release.gradle.
VERSION="$(sed -n 's/^ *releaseVersion = "\(.*\)"$/\1/p' release.gradle)"
[ -n "${VERSION}" ] || fail "Could not read releaseVersion from release.gradle."

## Run the release scripts, as a dry run, in a release directory of their own.
export CF_RELEASE_DIR="$TMP_DIR/cf-release"
mkdir -m 700 "$CF_RELEASE_DIR"
# The scripts run from a clone of the Checker Framework, and build its master branch: this checkout.
SCRIPTS_CLONE="$CF_RELEASE_DIR/checker-framework"
git clone --quiet "$ROOT_DIR" "$SCRIPTS_CLONE"
git -C "$SCRIPTS_CLONE" checkout --quiet -B master "$(git rev-parse HEAD)"
# Sign with a throw-away key.
export GNUPGHOME="$TMP_DIR/gnupg"
mkdir -m 700 "$GNUPGHOME"
SIGNING_KEY="release-test@example.invalid"
gpg --batch --pinentry-mode loopback --passphrase '' --quick-generate-key "$SIGNING_KEY" default default never
# Use a repository of its own, so that a release version is not left in the developer's ~/.m2.
export CF_RELEASE_MAVEN_REPO="$TMP_DIR/m2"
export JAVA_21_HOME="${JAVA_21_HOME:-${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}}"
# The release commit has the author of the checkout's commit, who is a contributor already.
GIT_AUTHOR_NAME="$(git log -1 --format=%an)"
GIT_AUTHOR_EMAIL="$(git log -1 --format=%ae)"
export GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL
export GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME" GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"
export CF_RELEASE_SIGNING_KEY="$SIGNING_KEY"
python3 "$SCRIPTS_CLONE/docs/developer/release/release_build.py" --dry-run
python3 "$SCRIPTS_CLONE/docs/developer/release/release_push.py" --dry-run
git -C "$CF_RELEASE_DIR/build/checker-framework" tag --verify "checker-framework-${VERSION}" \
  || fail "The release tag is not signed."

## Keep the zip files for the nightly build.
mkdir -p build
cp "$CF_RELEASE_DIR/release-files/checker-framework-${VERSION}.zip" \
  "$CF_RELEASE_DIR/release-files/annotation-file-utilities-${VERSION}.zip" build/

## Check the contents of the zip files.
CF_ZIP="build/checker-framework-${VERSION}.zip"
AFU_ZIP="build/annotation-file-utilities-${VERSION}.zip"
[ -f "$CF_ZIP" ] || fail "$CF_ZIP does not exist."
[ -f "$AFU_ZIP" ] || fail "$AFU_ZIP does not exist."
unzip -Z1 "$CF_ZIP" > "$TMP_DIR/cf-zip.txt"
unzip -Z1 "$AFU_ZIP" > "$TMP_DIR/afu-zip.txt"
for f in \
  checker/dist/checker.jar \
  checker/dist/checker-qual.jar \
  checker/bin/javac \
  README.md \
  docs/manual/manual.pdf \
  docs/manual/checker-framework-dataflow-manual.pdf \
  docs/tutorial/index.html \
  docs/tutorial/sourcefiles.zip; do
  grep -qx "checker-framework-${VERSION}/$f" "$TMP_DIR/cf-zip.txt" \
    || fail "The Checker Framework zip file does not contain $f."
done
for f in \
  annotation-file-utilities/dist/annotation-file-utilities.jar \
  annotation-file-utilities/dist/annotation-file-utilities-all.jar \
  annotation-file-utilities/bin/insert-annotations-to-source \
  annotation-file-utilities/annotation-file-utilities.html; do
  grep -qx "annotation-file-utilities-${VERSION}/$f" "$TMP_DIR/afu-zip.txt" \
    || fail "The Annotation File Utilities zip file does not contain $f."
done
# The two distributions are independent: neither contains the other's classes.
if grep -q 'annotation-file-utilities' "$TMP_DIR/cf-zip.txt"; then
  fail "The Checker Framework zip file contains the Annotation File Utilities."
fi
if grep -q 'checker/dist/checker.jar' "$TMP_DIR/afu-zip.txt"; then
  fail "The Annotation File Utilities zip file contains the Checker Framework."
fi

# The scripts that the manual tells users to run, such as wpi.sh, must be executable.
for f in checker/bin/javac checker/bin/wpi.sh checker/bin/wpi-many.sh checker/bin/infer-and-annotate.sh; do
  unzip -Z "$CF_ZIP" "checker-framework-${VERSION}/$f" | grep -q '^-rwx' \
    || fail "$f is not executable in the Checker Framework zip file."
done
unzip -Z "$AFU_ZIP" "annotation-file-utilities-${VERSION}/annotation-file-utilities/bin/insert-annotations-to-source" \
  | grep -q '^-rwx' || fail "insert-annotations-to-source is not executable in the Annotation File Utilities zip file."
# The manual shows its images: each one that manual.html references is next to it in the Checker
# Framework zip file.
grep -qx "checker-framework-${VERSION}/docs/manual/manual.html" "$TMP_DIR/cf-zip.txt" \
  || fail "The Checker Framework zip file does not contain docs/manual/manual.html."
mapfile -t MANUAL_IMAGES < <(unzip -p "$CF_ZIP" "checker-framework-${VERSION}/docs/manual/manual.html" | grep -o '<img [^>]*src="[^"]*"' | sed 's/.*src="//; s/"$//' | sort -u)
[ "${#MANUAL_IMAGES[@]}" -gt 0 ] || fail "No image found in docs/manual/manual.html."
for img in "${MANUAL_IMAGES[@]}"; do
  grep -qx "checker-framework-${VERSION}/docs/manual/$img" "$TMP_DIR/cf-zip.txt" \
    || fail "The Checker Framework zip file does not contain docs/manual/$img, which the manual shows."
done
# The manual shows the figures.
grep -q "^annotation-file-utilities-${VERSION}/annotation-file-utilities/figures/.*\.svg$" "$TMP_DIR/afu-zip.txt" \
  || fail "The Annotation File Utilities zip file does not contain the figures of its manual."

## Check the Annotation File Utilities jars: they have content, and they are under the MIT License.
UNPACKED_DIR="$TMP_DIR/unpacked"
mkdir -p "$UNPACKED_DIR"
unzip -q "$AFU_ZIP" -d "$UNPACKED_DIR"
unzip -q "$CF_ZIP" -d "$UNPACKED_DIR"
AFU_HOME="$UNPACKED_DIR/annotation-file-utilities-${VERSION}/annotation-file-utilities"
CF_HOME="$UNPACKED_DIR/checker-framework-${VERSION}"
check_afu_jar() {
  jar="$1"
  [ -f "$jar" ] || fail "$jar does not exist."
  unzip -Z1 "$jar" | grep -qx 'org/checkerframework/afu/annotator/Main.class' \
    || fail "$jar does not contain the class of the annotator."
  unzip -Z1 "$jar" | grep -qx 'org/checkerframework/afu/scenelib/io/IndexFileParser.class' \
    || fail "$jar does not contain the classes of the scene library."
  unzip -p "$jar" META-INF/LICENSE.txt | cmp -s - annotation-file-utilities/LICENSE.txt \
    || fail "$jar does not contain the license of the Annotation File Utilities."
  unzip -p "$jar" META-INF/MANIFEST.MF | grep -q '^Bundle-License: MIT' \
    || fail "$jar is not marked as under the MIT License."
}
check_afu_jar "$AFU_HOME/dist/annotation-file-utilities.jar"
check_afu_jar "$AFU_HOME/dist/annotation-file-utilities-all.jar"

# The stubifier source set is compiled separately from framework's main source set, and each jar
# gets it by its own route.  Checks that a jar has each entry point of the stubifier tool exactly
# once; the second argument is the file extension: class, java (a sources jar), or html (Javadoc).
check_stubifier_jar() {
  jar="$1"
  ext="$2"
  [ -f "$jar" ] || fail "$jar does not exist."
  entries="$(unzip -Z1 "$jar")"
  for c in JavaStubifier BinaryStubWriter BinaryStubFileGenerator; do
    count="$(grep -cx "org/checkerframework/framework/stubifier/$c\.$ext" <<< "$entries" || true)"
    [ "$count" = 1 ] || fail "$jar contains org/checkerframework/framework/stubifier/$c.$ext $count times, not once."
  done
}
check_stubifier_jar "$CF_HOME/checker/dist/checker.jar" class

## Run the Annotation File Utilities from the unpacked zip file.  release_push.py runs the
## Checker Framework from it.
SMOKE_DIR="$TMP_DIR/smoke"
mkdir -p "$SMOKE_DIR"
printf 'class Insert {\n  Object f;\n}\n' > "$SMOKE_DIR/Insert.java"
printf 'package org.checkerframework.checker.nullness.qual:\nannotation @Nullable:\n\npackage :\nclass Insert:\n    field f: @org.checkerframework.checker.nullness.qual.Nullable\n' > "$SMOKE_DIR/Insert.jaif"
(cd "$SMOKE_DIR" && "$AFU_HOME/bin/insert-annotations-to-source" -i Insert.jaif Insert.java)
grep -q '@Nullable' "$SMOKE_DIR/Insert.java" \
  || {
    cat "$SMOKE_DIR/Insert.java"
    fail "insert-annotations-to-source did not insert the annotation."
  }

## Check the published artifacts that release_build.py does not check.
MAVEN_REPO="$CF_RELEASE_MAVEN_REPO"
MAVEN_LOCAL="$MAVEN_REPO/io/github/eisop"
for artifact in framework framework-all checker; do
  prefix="$MAVEN_LOCAL/$artifact/${VERSION}/$artifact-${VERSION}"
  check_stubifier_jar "$prefix.jar" class
  check_stubifier_jar "$prefix-sources.jar" java
  check_stubifier_jar "$prefix-javadoc.jar" html
done
# The Annotation File Utilities do not depend on the checkers.
if grep -E '<artifactId>(checker|framework)</artifactId>' \
  "$MAVEN_LOCAL/annotation-file-utilities/${VERSION}/annotation-file-utilities-${VERSION}.pom"; then
  fail "The Annotation File Utilities POM depends on the Checker Framework."
fi
AFU_POM="$MAVEN_LOCAL/annotation-file-utilities/${VERSION}/annotation-file-utilities-${VERSION}.pom"
for expected in \
  "<groupId>io.github.eisop</groupId>" \
  "<artifactId>annotation-file-utilities</artifactId>" \
  "<version>${VERSION}</version>" \
  "<name>Annotation File Utilities</name>" \
  "<description>" \
  "<name>The MIT License</name>" \
  "<artifactId>asm</artifactId>" \
  "<artifactId>guava</artifactId>" \
  "<artifactId>plume-util</artifactId>" \
  "<artifactId>options</artifactId>" \
  "<artifactId>reflection-util</artifactId>"; do
  grep -qF "$expected" "$AFU_POM" || fail "The Annotation File Utilities POM does not contain $expected."
done
# The published jar contains only the Annotation File Utilities, and no dependencies.
if unzip -Z1 "$MAVEN_LOCAL/annotation-file-utilities/${VERSION}/annotation-file-utilities-${VERSION}.jar" \
  | grep -v -e '/$' -e '^META-INF/' -e '^org/checkerframework/afu/' | grep .; then
  fail "The Annotation File Utilities jar contains files outside org/checkerframework/afu."
fi

## Run the annotator from the published artifact, as a consumer project would.
(cd docs/examples/afu-consumer \
  && "$ROOT_DIR/gradlew" -Dmaven.repo.local="$MAVEN_REPO" -PafuVersion="${VERSION}" --console=plain \
    clean smoketest)

