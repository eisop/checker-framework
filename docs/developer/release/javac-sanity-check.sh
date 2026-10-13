#!/bin/bash

# Runs the Nullness Checker of an unpacked Checker Framework release on NullnessExampleWithWarnings.java
# and checks that it reports the expected errors.
#
# This script is run by sanity_checks.py (from release_push.py) on the downloaded release, and by
# checker/bin-devel/test-release.sh (in CI) on the release that it built.
#
# Usage: javac-sanity-check.sh <unpacked Checker Framework release> <NullnessExampleWithWarnings.java>

set -u

if [ $# -ne 2 ]; then
  echo "Usage: javac-sanity-check.sh <unpacked Checker Framework release> <NullnessExampleWithWarnings.java>" >&2
  exit 2
fi

CF_DIR="$1"
EXAMPLE="$2"
JAVAC="$CF_DIR/checker/bin/javac"

# The errors that the example is supposed to have.  Update the line numbers if the example changes.
EXPECTED_ERRORS=(
  "NullnessExampleWithWarnings.java:24: error: (assignment.type.incompatible)"
  "NullnessExampleWithWarnings.java:34: error: (argument.type.incompatible)"
)

status=0
# The second processor name tests the shorthand for built-in checkers.
# https://eisop.github.io/cf/manual/#shorthand-for-checkers
for processor in org.checkerframework.checker.nullness.NullnessChecker NullnessChecker; do
  # javac exits with an error status, because of the expected errors.  The caller may have set -e.
  output="$("$JAVAC" -processor "$processor" "$EXAMPLE" -Anomsgtext 2>&1)" || true
  for expected in "${EXPECTED_ERRORS[@]}"; do
    if ! grep -qF "$expected" <<< "$output"; then
      echo "Javac sanity check did not work! With -processor $processor the output should contain:" >&2
      echo "  $expected" >&2
      status=1
    fi
  done
  if [ "$status" -ne 0 ]; then
    echo "The output is:" >&2
    echo "$output" >&2
    exit "$status"
  fi
  echo "Javac sanity check with -processor $processor: passed!"
done
