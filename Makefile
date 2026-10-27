# This Makefile checks and enforces style.
# To build the EISOP Checker Framework, use Gradle.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

# Claude Code's skill files start with YAML front matter that the Markdown linter does not accept, and
# its local worktrees are not part of the project.  The Javadoc in docs/tmpapi and the manual in
# docs/manual/manual.html are generated, and `./gradlew htmlValidate` checks the HTML files that are not.
CODE_STYLE_EXCLUSIONS_USER := --exclude-dir=.claude --exclude-dir=tmpapi --exclude=manual.html --exclude=annotation-file-format.html

# Code style; defines `style-check` and `style-fix`.
# The clone runs with the GIT_* variables that git exports to a hook removed: when make runs from
# the pre-commit hook of a checkout without .plume-scripts, GIT_INDEX_FILE names this repository's
# index, and `git clone` would write the clone's tree into it (the index then names objects that
# only the clone has, and `git commit` fails with "invalid object ... Error building trees").
ifeq (,$(wildcard .plume-scripts))
dummy := $(shell env -u GIT_INDEX_FILE -u GIT_DIR -u GIT_WORK_TREE -u GIT_PREFIX git clone --depth=1 -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts)
endif
include .plume-scripts/code-style.mak
