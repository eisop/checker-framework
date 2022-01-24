#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
source "$SCRIPTDIR"/build.sh


## checker-framework-inference is a downstream test, but run it in its own
## script rather than in ./test/downstream.sh, because it needs a different
## Docker image.

"$SCRIPTDIR/.plume-scripts/git-clone-related" opprop checker-framework-inference

export PATH=$AFU/scripts:$PATH
cd ../checker-framework-inference
./.ci-build.sh
