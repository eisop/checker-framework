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

## HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all

## Javadoc documentation
# Try twice in case of network lossage.
(./gradlew javadoc --console=plain --warning-mode=all || (sleep 60 && ./gradlew javadoc --console=plain --warning-mode=all)) || status=1
./gradlew javadocPrivate --console=plain --warning-mode=all || status=1
# For refactorings that touch a lot of code that you don't understand, create
# top-level file SKIP-REQUIRE-JAVADOC.  Delete it after the pull request is merged.
if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping requireJavadoc because file SKIP-REQUIRE-JAVADOC exists."
else
  (./gradlew requireJavadoc --console=plain --warning-mode=all > /tmp/warnings-rjp.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-rjp.txt || status=1
  (./gradlew javadocDoclintAll --console=plain --warning-mode=all > /tmp/warnings-jda.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-jda.txt || status=1
fi
if [ $status -ne 0 ]; then exit $status; fi

# Shell script style
make -C checker/bin --jobs="$(getconf _NPROCESSORS_ONLN)" shell-script-style
make -C checker/bin-devel --jobs="$(getconf _NPROCESSORS_ONLN)" shell-script-style

## User documentation
./gradlew manual
git diff --exit-code docs/manual/contributors.tex || \
    (set +x && set +v &&
     echo "docs/manual/contributors.tex is not up to date." &&
     echo "If the above suggestion is appropriate, run: make -C docs/manual contributors.tex" &&
     echo "If the suggestion contains a username rather than a human name, then do all the following:" &&
     echo "  * Update your git configuration by running:  git config --global user.name \"YOURFULLNAME\"" &&
     echo "  * Add your name to your GitHub account profile at https://github.com/settings/profile" &&
     echo "  * Make a pull request to add your GitHub ID to" &&
     echo "    https://github.com/eisop-plume-lib/git-scripts/blob/master/git-authors.sed" &&
     echo "    and remake contributors.tex after that pull request is merged." &&
     false)

# Check the definition of qualifiers in Checker Framework against the JDK
echo "Checking the definition of qualifiers in Checker Framework against the JDK"
CURRENT_PATH=$(pwd)
src_dir="$CURRENT_PATH/checker-qual/src/main/java/org/checkerframework"
jdk_dir="$CURRENT_PATH/../jdk/src/java.base/share/classes/org/checkerframework"
set +o xtrace  # Turn off xtrace to avoid verbose output

difference_found=false
file_missing_in_jdk=false
file_removed_in_cf=false

while read -r file; do
    # Use parameter expansion to get the relative path of the file
    # e.g. rel_path= "checker/nullness/qual/Nullable.java"
    rel_path="${file#"$src_dir"/}"
    jdk_file="$jdk_dir/$rel_path"

    # Check if the file exists in the JDK directory
    if [ ! -f "$jdk_file" ]; then
        echo "File missing in JDK: $rel_path"
        file_missing_in_jdk=true
    else
        diff_output=$(diff -q "$file" "$jdk_file" || true)

        if [ "$diff_output" ]; then
            echo "Difference found in: $rel_path"
            diff "$file" "$jdk_file" || true  # Show the full diff

            difference_found=true
        fi
    fi
done < <(find "$src_dir" -name "*.java")

# Check for files in the JDK that are not in CF
while read -r jdk_file; do
    rel_path="${jdk_file#"$jdk_dir"/}"
    cf_file="$src_dir/$rel_path"

    if [ ! -f "$cf_file" ]; then
        echo "File removed in CF: $rel_path"
        file_removed_in_cf=true
    fi
done < <(find "$jdk_dir" -name "*.java")

# If any difference, missing, or removed file was found, exit with failure
if [ "$difference_found" = true ] || [ "$file_missing_in_jdk" = true ] || [ "$file_removed_in_cf" = true ]; then
    echo "Differences found or files missing/removed. Exiting with failure."
    exit 1  # Exit with failure
else
    echo "No differences found and no files missing/removed."
fi

set -o xtrace  # Turn on xtrace output
