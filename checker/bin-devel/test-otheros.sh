#!/bin/bash

# Tests for Windows and macOS: the code that depends on the operating system, such as paths,
# class paths, and jar and directory lookups, through the entry points that users run.  The type
# rules themselves are platform-independent and are tested on Linux.

# Run every step even if an earlier one fails, to see all failures of a run; fail at the end.
status=0
# set -o verbose
set -o xtrace

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh
# clone-related.sh exports SHELLOPTS. /bin/sh is bash on Windows and macOS, so checker/bin/javac
# would inherit xtrace and write its trace into the output that the tests compare.
export -n SHELLOPTS

# checker/bin/javac and CheckerMain, and loading qualifiers from a directory and from a jar.
./gradlew --continue :checker:commandLineTests :framework:loaderTests --warning-mode=all || status=1

# JUnit tests that pass file paths: stub files on the command line and on the class path.
./gradlew :checker:test --tests '*.NullnessStubfileTest' --tests '*.StubparserNullnessTest' \
  --tests '*.BinaryStubClasspathTest' :framework:test --tests '*.ValueTest' --warning-mode=all \
  || status=1

# The examples build user projects with checker/bin/javac, Maven, Gradle, and Bazel.
# docs/examples/publish-smoketest/ needs the artifacts in the local Maven repository.  addFavicon
# runs a script that Windows cannot execute, and only post-processes the Javadoc.
./gradlew publishToMavenLocal -x javadoc -x allJavadoc -x addFavicon --warning-mode=all || status=1
./gradlew :checker:exampleTests --warning-mode=all || status=1

exit $status
