#!/bin/bash
# Record a JFR trace of the EISOP Checker Framework on a given workload.
#
# Usage:
#   record-jfr.sh -o FILE.jfr -- ./gradlew :checker:NullnessTest -PmaxParallelForks=1
#   record-jfr.sh -o FILE.jfr --javac -- ./checker/bin/javac -processor nullness src/Foo.java
#
# Modes:
#   default       — append JFR start flags to GRADLE_OPTS (for gradle runs).
#                    Caller must pass -PmaxParallelForks=1 explicitly; otherwise
#                    parallel test workers corrupt the JFR constant pool and
#                    the trace will not parse.
#   --javac       — prepend -J-XX:StartFlightRecording flags to the next argv.
#                    Use this when invoking checker/bin/javac directly or any
#                    other launcher that supports -J pass-through.
#
# The settings file at checker/bin-devel/profile-cf.jfc is tuned for CF
# workloads (10 ms sampling floor; expensive GC and exception events
# disabled). See that file's header for the rationale.
#
# Why this script exists:
#   - Parallel Gradle test workers corrupt JFR if they share a filename.
#     We force the user to opt into the right Gradle property.
#   - Direct javac invocations need -J-prefixed flags; the equivalent flags
#     for the launcher JVM produce a useless trace of the launcher startup.
#   - The stock profile.jfc produces 40+ MB traces dominated by GC events
#     that are useless for CF profiling. Our .jfc shrinks them to a couple
#     of MB without losing ExecutionSample resolution.
#   - The 10 ms floor is silently enforced by jfrThreadSampler.cpp; asking
#     for less just confuses the operator.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
JFC_FILE="$SCRIPT_DIR/profile-cf.jfc"

usage() {
  cat >&2 << 'EOF'
Usage: record-jfr.sh -o OUT.jfr [--javac] [--duration N] -- <command> [args...]

Options:
  -o OUT.jfr      Output JFR file. MUST be an absolute path or a path relative
                  to the launching directory; for multi-module Maven builds
                  prefer an absolute path.
  --javac         Prepend JFR -J flags to the command (use with checker/bin/javac
                  or any -J-aware launcher).
  --duration N    Recording duration in seconds; default 600 (10 minutes).
                  Pass 0 for "until JVM exits".
  -h, --help      Show this help.

Examples:
  record-jfr.sh -o /tmp/cf.jfr -- \
      ./gradlew :checker:NullnessTest -PmaxParallelForks=1

  record-jfr.sh -o /tmp/cf.jfr --javac -- \
      ./checker/bin/javac -processor nullness MyFile.java
EOF
  exit 2
}

OUT=""
DURATION=600
JAVAC_MODE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o)
      OUT="$2"
      shift 2
      ;;
    --javac)
      JAVAC_MODE=1
      shift
      ;;
    --duration)
      DURATION="$2"
      shift 2
      ;;
    -h | --help) usage ;;
    --)
      shift
      break
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      ;;
  esac
done

if [[ -z "$OUT" ]]; then
  echo "error: -o OUT.jfr is required" >&2
  usage
fi

if [[ $# -eq 0 ]]; then
  echo "error: no command after --" >&2
  usage
fi

if [[ ! -f "$JFC_FILE" ]]; then
  echo "error: settings file not found: $JFC_FILE" >&2
  exit 1
fi

# Absolute path: multi-module builds run javac in each module's working
# directory, so a relative filename produces N traces in N places.
case "$OUT" in
  /*) ;;
  *) OUT="$(pwd)/$OUT" ;;
esac

DUR_CLAUSE=""
if [[ "$DURATION" != "0" ]]; then
  DUR_CLAUSE="duration=${DURATION}s,"
fi

# stackdepth is a -XX:FlightRecorderOptions option, NOT a -XX:StartFlightRecording
# option. Putting it on StartFlightRecording makes the JVM print
#   "The .jfc option/setting 'stackdepth' doesn't exist."
# and silently fall back to the default depth of 64, which truncates the
# leaf-most callers of the deep visitor + dataflow + qualifier-hierarchy stacks
# this profile is meant to capture. It must be set separately.
RECORDER_OPTS="-XX:FlightRecorderOptions=stackdepth=256"

if [[ $JAVAC_MODE -eq 1 ]]; then
  # Single forked javac: it is the only JVM, so a fixed filename is unambiguous.
  JFR_FLAG="-XX:StartFlightRecording=${DUR_CLAUSE}settings=${JFC_FILE},filename=${OUT},dumponexit=true"
  # Prepend -J-prefixed flags to the next command.
  CMD=("$1")
  shift
  exec "${CMD[@]}" "-J${RECORDER_OPTS}" "-J${JFR_FLAG}" "$@"
else
  # Gradle / generic-JVM mode: JAVA_TOOL_OPTIONS is inherited by *every* JVM the
  # build spawns — the Gradle launcher, the daemon, the test worker, and any
  # forked javac. If they all share one filename= they clobber each other (last
  # writer wins) and shred the constant pool, so the trace comes back with
  # <null> thread names and is dominated by the launcher's idle frames
  # (EPoll.wait, ProcessHandleImpl.waitForProcessExit0) rather than the
  # compilation work. Use the JFR %p filename token so each JVM writes its own
  # file; the worker that does the type-checking is the largest one.
  #
  # We intentionally do NOT set GRADLE_OPTS: recording the launcher/daemon JVM
  # only adds idle-trace noise files. JAVA_TOOL_OPTIONS alone reaches the worker.
  case "$OUT" in
    *%p*) OUT_PATTERN="$OUT" ;;
    *.jfr) OUT_PATTERN="${OUT%.jfr}-%p.jfr" ;;
    *) OUT_PATTERN="${OUT}-%p" ;;
  esac
  JFR_FLAG="-XX:StartFlightRecording=${DUR_CLAUSE}settings=${JFC_FILE},filename=${OUT_PATTERN},dumponexit=true"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} ${RECORDER_OPTS} ${JFR_FLAG}"
  echo "JFR flags will be applied via JAVA_TOOL_OPTIONS:" >&2
  echo "  ${RECORDER_OPTS} ${JFR_FLAG}" >&2
  echo "Each JVM writes its own file (the %p PID token). The type-checking" >&2
  echo "worker is the largest file; analyze that one. Other files (launcher," >&2
  echo "daemon) are idle-trace noise and can be deleted." >&2
  echo "Reminder: Gradle test runs need -PmaxParallelForks=1 so there is a" >&2
  echo "single worker rather than several sharing the workload." >&2
  exec "$@"
fi
