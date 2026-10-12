#!/bin/bash

# Runs the steps of the release build that need no credentials, so that a problem in them is found
# by a pull request rather than during a release.  See docs/developer/release/release_build.py.
#
# The release scripts themselves are not run: they clone the repositories, ask questions, and push.
# This script runs the Gradle tasks that they run, with a throw-away signing key and into
# temporary directories, and checks the results.

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

# The version of a release build, which is the one in release.gradle.
VERSION="$(./gradlew -q :checker:properties -Prelease=true | sed -n 's/^version: //p')"
if [ -z "${VERSION}" ] || [[ "${VERSION}" == *SNAPSHOT* ]]; then
  fail "Could not determine the release version; got '${VERSION}'."
fi

## Update the version numbers in the documentation.
./gradlew updateVersionNumbers --console=plain --warning-mode=all
if git status --porcelain | grep -v ' docs/'; then
  fail "updateVersionNumbers changed files outside docs/."
fi
grep -q "annotation-file-utilities-${VERSION}" docs/annotation-file-utilities/annotation-file-utilities.html \
  || fail "updateVersionNumbers did not update the version in the Annotation File Utilities manual."
grep -q "checker-framework-${VERSION}" docs/checker-framework-webpage.html \
  || fail "updateVersionNumbers did not update the version in the web page."

## Build the release files.
./gradlew buildAll -Prelease=true --console=plain --warning-mode=all

## Copy the files to the web site directory.  Do it twice, as a release may be rerun.
WEBSITE_DIR="$TMP_DIR/website"
mkdir -p "$WEBSITE_DIR"
./gradlew copyToWebsite -Prelease=true -PcfWebsite="$WEBSITE_DIR" --console=plain --warning-mode=all
./gradlew copyToWebsite -Prelease=true -PcfWebsite="$WEBSITE_DIR" --rerun --console=plain --warning-mode=all

for f in \
  "checker-framework-${VERSION}.zip" \
  "annotation-file-utilities-${VERSION}.zip" \
  CHANGELOG.md \
  checker-framework-webpage.html \
  api/index.html \
  manual/manual.html \
  manual/developer-manual.html \
  annotation-file-utilities/annotation-file-utilities.html \
  annotation-file-utilities/annotation-file-format.html; do
  [ -f "$WEBSITE_DIR/$f" ] || fail "copyToWebsite did not create $f."
done
for link in index.html manual/index.html annotation-file-utilities/index.html; do
  [ -L "$WEBSITE_DIR/$link" ] || fail "copyToWebsite did not create the symbolic link $link."
  [ -f "$WEBSITE_DIR/$link" ] || fail "The symbolic link $link points to a missing file."
done
# A file in the web site must stay writable, so that the next release can replace it.
if find "$WEBSITE_DIR" -type f ! -perm -u+w | grep .; then
  fail "copyToWebsite created read-only files."
fi

## Check the contents of the zip files.
CF_ZIP="$WEBSITE_DIR/checker-framework-${VERSION}.zip"
AFU_ZIP="$WEBSITE_DIR/annotation-file-utilities-${VERSION}.zip"
unzip -Z1 "$CF_ZIP" > "$TMP_DIR/cf-zip.txt"
unzip -Z1 "$AFU_ZIP" > "$TMP_DIR/afu-zip.txt"
for f in checker/dist/checker.jar checker/dist/checker-qual.jar checker/bin/javac README.md; do
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

## Run the tools from the unpacked zip files.
SMOKE_DIR="$TMP_DIR/smoke"
mkdir -p "$SMOKE_DIR"
cp docs/examples/NullnessExampleWithWarnings.java "$SMOKE_DIR"
if (cd "$SMOKE_DIR" && "$CF_HOME/checker/bin/javac" -processor nullness NullnessExampleWithWarnings.java 2> javac.out); then
  fail "The Nullness Checker found no error in NullnessExampleWithWarnings.java."
fi
grep -q 'NullnessExampleWithWarnings.java.*error' "$SMOKE_DIR/javac.out" \
  || {
    cat "$SMOKE_DIR/javac.out"
    fail "The Nullness Checker did not report the expected errors."
  }
printf 'class Insert {\n  Object f;\n}\n' > "$SMOKE_DIR/Insert.java"
printf 'package org.checkerframework.checker.nullness.qual:\nannotation @Nullable:\n\npackage :\nclass Insert:\n    field f: @org.checkerframework.checker.nullness.qual.Nullable\n' > "$SMOKE_DIR/Insert.jaif"
(cd "$SMOKE_DIR" && "$AFU_HOME/bin/insert-annotations-to-source" -i Insert.jaif Insert.java)
grep -q '@Nullable' "$SMOKE_DIR/Insert.java" \
  || {
    cat "$SMOKE_DIR/Insert.java"
    fail "insert-annotations-to-source did not insert the annotation."
  }

## Publish to a local Maven repository, signing with a throw-away key.
export GNUPGHOME="$TMP_DIR/gnupg"
mkdir -m 700 "$GNUPGHOME"
SIGNING_KEY="release-test@example.invalid"
gpg --batch --pinentry-mode loopback --passphrase '' --quick-generate-key "$SIGNING_KEY" default default never
# Use a repository of its own, so that a release version is not left in the developer's ~/.m2.
MAVEN_REPO="$TMP_DIR/m2"
./gradlew publishToMavenLocal -Prelease=true -Dmaven.repo.local="$MAVEN_REPO" \
  -Psigning.gnupg.keyName="$SIGNING_KEY" -Psigning.gnupg.passphrase= --console=plain --warning-mode=all

MAVEN_LOCAL="$MAVEN_REPO/io/github/eisop"
for artifact in annotation-file-utilities checker checker-qual checker-util framework dataflow javacutil; do
  dir="$MAVEN_LOCAL/$artifact/${VERSION}"
  for suffix in .pom .jar .jar.asc .pom.asc; do
    [ -f "$dir/$artifact-${VERSION}$suffix" ] || fail "Maven publication is missing $dir/$artifact-${VERSION}$suffix."
  done
done
# The Annotation File Utilities do not depend on the checkers.
if grep -E '<artifactId>(checker|framework)</artifactId>' \
  "$MAVEN_LOCAL/annotation-file-utilities/${VERSION}/annotation-file-utilities-${VERSION}.pom"; then
  fail "The Annotation File Utilities POM depends on the Checker Framework."
fi
