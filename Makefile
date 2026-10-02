# This Makefile checks and enforces style.
# To build the EISOP Checker Framework, use Gradle.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

# Claude Code's skill files start with YAML front matter that the Markdown linter does not accept, and
# its local worktrees are not part of the project.
CODE_STYLE_EXCLUSIONS_USER := --exclude-dir=.claude

# Code style; defines `style-check` and `style-fix`.
ifeq (,$(wildcard .plume-scripts))
dummy := $(shell git clone --depth=1 -q https://github.com/eisop-plume-lib/plume-scripts.git .plume-scripts)
endif
include .plume-scripts/code-style.mak
