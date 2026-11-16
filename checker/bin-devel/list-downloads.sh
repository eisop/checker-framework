#!/bin/bash

# Lists the files under the download caches that are newer than the marker file $1, which CI
# creates before it restores the caches: the files that the job downloaded or created.

marker="$1"
for root in "$HOME/.gradle/caches/modules-2/files-2.1" "$HOME/.gradle/wrapper/dists" \
  "$HOME/.m2/repository" "$HOME/.cache" "$HOME/.bazelisk" "${GITHUB_WORKSPACE:-.}/../jtreg"; do
  [ -d "$root" ] || continue
  files=$(find "$root" -type f -newer "$marker" 2> /dev/null)
  n=$(printf '%s' "$files" | grep -c . || true)
  echo "=== $root: $n new files"
  [ "$n" = 0 ] && continue
  size=$(printf '%s\n' "$files" | tr '\n' '\0' | du -ch --files0-from=- 2> /dev/null | tail -1 | cut -f1)
  echo "    total size $size"
  case "$root" in
    */files-2.1)
      # group/artifact/version
      printf '%s\n' "$files" | sed "s|^$root/||" | cut -d/ -f1-3 | sort -u | sed 's|/|:|g; s|^|    |'
      ;;
    */repository)
      # group path/artifact/version
      printf '%s\n' "$files" | sed "s|^$root/||" | xargs -n1 dirname | sort -u | sed 's|^|    |'
      ;;
    */dists)
      printf '%s\n' "$files" | sed "s|^$root/||" | cut -d/ -f1 | sort -u | sed 's|^|    |'
      ;;
    *)
      printf '%s\n' "$files" | sed "s|^$root/||" | cut -d/ -f1-3 | sort | uniq -c | sed 's|^|  |'
      ;;
  esac
done
