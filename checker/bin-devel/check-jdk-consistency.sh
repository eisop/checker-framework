#!/bin/bash

reset_trace=false
if [ -o xtrace ]; then
    # Turn off xtrace to avoid verbose output
    set +o xtrace
    reset_trace=true
fi

# Check the definition of qualifiers in Checker Framework against the JDK
# This script assume JDK is cloned and located in the parent directory of the Checker Framework
echo "Checking the definition of qualifiers in Checker Framework against the JDK"
CURRENT_PATH=$(pwd)
src_dir="$CURRENT_PATH/checker-qual/src/main/java/org/checkerframework"
jdk_dir="$CURRENT_PATH/../jdk/src/java.base/share/classes/org/checkerframework"

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

if [ "$reset_trace" = true ]; then
    # Turn on xtrace output
    set -o xtrace
fi

# If any difference, missing, or removed file was found, exit with failure
if [ "$difference_found" = true ] || [ "$file_missing_in_jdk" = true ] || [ "$file_removed_in_cf" = true ]; then
    echo "Differences found or files missing/removed. Exiting with failure."
    exit 1  # Exit with failure
else
    echo "No differences found and no files missing/removed."
fi
