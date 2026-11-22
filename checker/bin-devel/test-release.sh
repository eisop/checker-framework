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
# The changelog has a section for the release.  Its date may still be a placeholder here; the
# release build (release_build.py) checks the date.
grep -q "^## Version ${VERSION} (" docs/CHANGELOG.md \
  || fail "docs/CHANGELOG.md has no section \"## Version ${VERSION} (...)\"."

## Build the release files.
./gradlew buildAll -Prelease=true --console=plain --warning-mode=all
# The source files of the tutorial, which its web page links to.
make -C docs/tutorial

## Create the zip files, which release_build.py attaches to the GitHub release.  The web site is
## generated from them.
./gradlew zip annotationFileUtilitiesZip -Prelease=true --console=plain --warning-mode=all

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

## Check the links of the pages that the zip ships.  The check of external links is online, so it
## is left to the release build (release_build.py); here they are only checked for syntax.
checker/bin-devel/check-release-links.py "$CF_HOME"

## Run the tools from the unpacked zip files.
SMOKE_DIR="$TMP_DIR/smoke"
mkdir -p "$SMOKE_DIR"
# The same check that release_push.py runs on the release zip file.
docs/developer/release/javac-sanity-check.sh "$CF_HOME" "$CF_HOME/docs/examples/NullnessExampleWithWarnings.java"
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

## No published POM depends on org.checkerframework:checker-qual, the typetools artifact: a
## consumer must get io.github.eisop:checker-qual only.  (Exclusions of it are fine.)
for pom in "$MAVEN_LOCAL"/*/"${VERSION}"/*.pom; do
  python3 - "$pom" << 'EOF' || fail "$pom depends on org.checkerframework:checker-qual."
import sys
import xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
for dep in ET.parse(sys.argv[1]).getroot().findall("m:dependencies/m:dependency", ns):
    if dep.findtext("m:groupId", "", ns) == "org.checkerframework" and dep.findtext("m:artifactId", "", ns) == "checker-qual":
        sys.exit(1)
EOF
done

## Compile the Maven example against the published artifacts, as release_push.py does: the
## checker runs from the annotation processor path and reports the example's expected error.
MAVEN_EXAMPLE_DIR="$TMP_DIR/maven-example"
./gradlew updateCopyMavenExample -Prelease=true -PdestDir="$MAVEN_EXAMPLE_DIR" --console=plain --warning-mode=all
# The example's build is supposed to fail; `-fn` makes mvn exit normally and the grep decides.
(cd "$MAVEN_EXAMPLE_DIR/MavenExample" \
  && mvn -B -fn -Dmaven.repo.local="$MAVEN_REPO" compile > "$TMP_DIR/maven-example.txt" 2>&1) || true
grep -Eq 'MavenExample\.java:\[30,(29|30)\] \[assignment\.type\.incompatible\] incompatible types in assignment\.' \
  "$TMP_DIR/maven-example.txt" \
  || {
    cat "$TMP_DIR/maven-example.txt"
    fail "The Maven example did not report its expected error with the published artifacts."
  }
