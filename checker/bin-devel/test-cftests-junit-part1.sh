#!/bin/bash

# Part 1 of the JUnit tests; see test-cftests-junit.sh.

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
exec "$SCRIPT_DIR"/test-cftests-junit.sh part1
