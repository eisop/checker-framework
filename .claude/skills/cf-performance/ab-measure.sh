#!/usr/bin/env bash
# A/B measurement driver for one build side: runs `checker/bin/javac -processor nullness`
# over each given source file REPS times and reports the MEDIAN total allocation
# (deterministic jdk.ThreadAllocationStatistics, ~0.15% reproducible) and the MEDIAN wall
# clock. Allocation and wall are measured in SEPARATE runs, because JFR recording perturbs
# timing (see SKILL.md "Measuring wall-clock effects").
#
# This measures whatever is currently built into checker/dist. To A/B two sides:
#   git checkout <baseline>  && ./gradlew assembleForJavac && ab-measure.sh -l master Big300.java Varargs.java
#   git checkout <treatment> && ./gradlew assembleForJavac && ab-measure.sh -l branch Big300.java Varargs.java
# then diff the two tables. (Rebuild the dist on each side: the forked javac uses it.)
#
# Note: deterministic allocation is BLIND to pure-CPU/traversal wins (a change that does
# fewer scans but allocates the same shows flat here) -- for those, compare wall and/or
# on-CPU samples, or instrument an operation counter. See SKILL.md.
#
# Usage: ab-measure.sh [-n REPS] [-l LABEL] <File.java> [File2.java ...]

set -euo pipefail

REPS=3
LABEL="side"
while getopts "n:l:" opt; do
  case "$opt" in
    n) REPS="$OPTARG" ;;
    l) LABEL="$OPTARG" ;;
    *)
      echo "usage: ab-measure.sh [-n REPS] [-l LABEL] <File.java>..." >&2
      exit 2
      ;;
  esac
done
shift $((OPTIND - 1))
[ "$#" -ge 1 ] || {
  echo "usage: ab-measure.sh [-n REPS] [-l LABEL] <File.java>..." >&2
  exit 2
}

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SKILL_DIR/../../.." && pwd)"
JAVAC="$REPO/checker/bin/javac"
ALLOC="$SKILL_DIR/alloc-total.java"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

[ -x "$JAVAC" ] || {
  echo "no checker/bin/javac at $JAVAC -- run ./gradlew assembleForJavac" >&2
  exit 1
}

median() { python3 -c "import sys; xs=sorted(float(x) for x in sys.argv[1:]); print(f'{xs[len(xs)//2]:.2f}')" "$@"; }

printf '### SIDE=%s  reps=%d ###\n' "$LABEL" "$REPS"
printf '%-28s %14s %10s\n' "program" "alloc(MB,med)" "wall(s,med)"
for prog in "$@"; do
  allocs=()
  for _ in $(seq "$REPS"); do
    "$JAVAC" -J-XX:StartFlightRecording=settings=profile,filename="$TMP/r.jfr",dumponexit=true \
      -processor nullness -d "$TMP/out" "$prog" > /dev/null 2>&1 || true
    mb="$(java "$ALLOC" "$TMP/r.jfr" 2> /dev/null \
      | grep -oE '= [0-9.]+ MB' | grep -oE '[0-9.]+' || echo 0)"
    allocs+=("$mb")
  done
  walls=()
  for _ in $(seq "$REPS"); do
    t="$(python3 -c "
import subprocess,time
t=time.time()
subprocess.run(['$JAVAC','-processor','nullness','-d','$TMP/out','$prog'],
               stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
print(f'{time.time()-t:.2f}')")"
    walls+=("$t")
  done
  printf '%-28s %14s %10s\n' "$(basename "$prog")" "$(median "${allocs[@]}")" "$(median "${walls[@]}")"
done
