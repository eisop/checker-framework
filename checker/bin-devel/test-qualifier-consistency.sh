#!/bin/bash

# Check the definition of qualifiers in Checker Framework against the JDK
current_path=$(pwd)
src_dir="$current_path/checker-qual/src/main/java/org/checkerframework"
jdk_dir="$current_path/../jdk/src/java.base/share/classes/org/checkerframework"

difference_found=false

for file in $(find "$src_dir" -name "*.java"); do
    rel_path="${file#"$src_dir"/}"
    jdk_file="$jdk_dir/$rel_path"

    if [ -f "$jdk_file" ]; then
        diff_output=$(diff -q "$file" "$jdk_file")

        if [ "$diff_output" ]; then
            echo "============================="
            echo "Difference found in: $rel_path"
            echo "-----------------------------"
            diff "$file" "$jdk_file" | tail -n 10

            difference_found=true
        fi
    fi
done

# If any difference was found, exit with a non-zero status
if [ "$difference_found" = true ]; then
    echo "Differences found. Exiting with failure."
    exit 1  # Exit with failure
else
    echo "No differences found."
    exit 0  # Exit with success
fi
