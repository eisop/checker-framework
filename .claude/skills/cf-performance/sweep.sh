#!/usr/bin/env bash
# Super-linearity size-sweep driver. Companion to gen-shapes.py.
#
# Usage:  sweep.sh <shape> <reps> <D...>
#   e.g.  .claude/skills/cf-performance/sweep.sh cond 60 20 40 80 160
#
# For each D it generates the shape, compiles it with the currently-built checker/bin/javac
# (-processor nullness, JFR dumponexit), reads deterministic total allocation via alloc-total.java
# (jdk.ThreadAllocationStatistics; takes the min of 2 reps), and prints alloc plus the MARGINAL
# Delta(alloc)/Delta(D). Read the marginal column, not the absolute: roughly CONSTANT marginal =
# linear; marginal that DOUBLES each time D doubles = quadratic; quadruples = cubic. Always sweep
# the `control` shape too -- it calibrates that the harness reports linear as linear.
#
# It also prints the wall time (the min of the 2 reps, taken while JFR records).
#
# SWEEP_ARGS replaces the default "-processor nullness", for example to sweep another checker or
# the JSpecify reference checker on shapes generated with --jspecify (SWEEP_GEN_ARGS=--jspecify):
#   SWEEP_GEN_ARGS=--jspecify SWEEP_ARGS="-cp $CP -processorpath $CP \
#     -processor com.google.jspecify.nullness.NullSpecChecker" sweep.sh inner 20 2 4 8 16
#
# Requires checker/bin/javac to be built for the side under test (./gradlew assembleForJavac).
set -u
read -r -a args <<< "${SWEEP_ARGS:--processor nullness}"
read -r -a gen_args <<< "${SWEEP_GEN_ARGS:-}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
shape=$1
reps=$2
shift 2
prev=""
prevd=""
for d in "$@"; do
  src=/tmp/s_${shape}_${d}.java
  python3 "$here/gen-shapes.py" "$d" --shape "$shape" --reps "$reps" "${gen_args[@]}" > "$src"
  best=""
  bestw=""
  for _ in 1 2; do
    start=$(date +%s.%N)
    ./checker/bin/javac -J-XX:StartFlightRecording=settings=profile,filename=/tmp/s.jfr,dumponexit=true \
      "${args[@]}" -d "/tmp/so_$shape" "$src" > /tmp/serr.txt 2>&1
    wall=$(echo "$(date +%s.%N) - $start" | bc)
    if grep -qiE "stackoverflow" /tmp/serr.txt; then
      printf "%-8s D=%-4s STACK-OVERFLOW (nesting too deep)\n" "$shape" "$d"
      best=""
      break
    fi
    bytes=$(java "$here/alloc-total.java" /tmp/s.jfr 2> /dev/null \
      | grep -oE '= [0-9,]+ bytes' | head -1 | tr -cd '0-9')
    [ -z "$bytes" ] && continue
    if [ -z "$best" ] || [ "$bytes" -lt "$best" ]; then best=$bytes; fi
    if [ -z "$bestw" ] || [ "$(echo "$wall < $bestw" | bc)" = 1 ]; then bestw=$wall; fi
  done
  [ -z "$best" ] && {
    prev=""
    prevd=""
    continue
  }
  mb=$(echo "scale=1; $best/1048576" | bc)
  errs=$(grep -c "error:" /tmp/serr.txt)
  if [ -n "$prev" ]; then
    marg=$(echo "scale=1; ($best-$prev)/($d-$prevd)/1024" | bc)
    printf "%-8s D=%-4s alloc=%9s MB  marginal=%9s KB/unit  wall=%6.1f s  (err=%s)\n" \
      "$shape" "$d" "$mb" "$marg" "$bestw" "$errs"
  else
    printf "%-8s D=%-4s alloc=%9s MB  marginal=        -          wall=%6.1f s  (err=%s)\n" \
      "$shape" "$d" "$mb" "$bestw" "$errs"
  fi
  prev=$best
  prevd=$d
done
