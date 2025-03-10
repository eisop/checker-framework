#!/bin/bash

# Define your directories
current_path=$(pwd)
src_dir="$current_path/checker-qual/src/main/java/org/checkerframework"
jdk_dir="$current_path/../jdk/src/java.base/share/classes/org/checkerframework"

# Variable to track if differences are found
difference_found=false

# Iterate through each Java file in the source directory
for file in $(find "$src_dir" -name "*.java"); do
    rel_path="${file#"$src_dir"/}"
    jdk_file="$jdk_dir/$rel_path"

    # Check if the file exists in the JDK repository
    if [ -f "$jdk_file" ]; then
        # Compare the two files and capture the diff
        diff_output=$(diff -q "$file" "$jdk_file")

        # If there's a difference, print it in the desired format and set difference_found to true
        if [ "$diff_output" ]; then
            echo "============================="
            echo "Difference found in: $rel_path"
            echo "-----------------------------"
            diff "$file" "$jdk_file" | tail -n 10  # Only show the last few lines of the diff

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
