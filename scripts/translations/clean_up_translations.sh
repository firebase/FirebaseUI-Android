#!/usr/bin/env bash
# This script should be run after a new round of translations

# Directory where this script lives
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Find all non-default strings files
function find_files {
    find . -path "*/src/main/res/values-*" -type f -name "string*.xml"
}

# Move locales that our system misconfigured
chmod -R +wx auth/src/main/res/values-*
rm -rf auth/src/main/res/values-{he,id}

# Remove all RTL locales (until we fully QA them)
# [Arabic and Hebrew]
rm -rf auth/src/main/res/values-ar
rm -rf auth/src/main/res/values-he
rm -rf auth/src/main/res/values-iw

# Process each file
find_files | while read file;
do
    echo "Processing: $file"

    # Remove non-translatable strings
    python $DIR/remove_non_translatable.py $file
done
