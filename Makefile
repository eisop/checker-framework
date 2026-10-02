# NOTE: this Makefile is only for style checks.
# To build the EISOP Checker Framework, use Gradle.

all default: style-check
	$(MAKE) -C .azure
	$(MAKE) -C checker/bin-devel

style-fix: markdownlint-fix python-style-fix shell-style-fix
# NOTE: markdownlint-check is not part of style-check yet: the Markdown files of this repository
# have not been made to conform to the markdownlint configuration.
style-check: python-style-check shell-style-check


markdownlint-fix:
	markdownlint-cli2 --fix "**/*.md" "#node_modules"
markdownlint-check:
	markdownlint-cli2 "**/*.md" "#node_modules"

PYTHON_FILES:=$(shell find . \( -name .do-like-javac -o -name .git-scripts -o -name .plume-scripts \) -prune -o -name "*.py" -print)
install-ruff:
	@if ! command -v ruff ; then pipx install ruff ; fi
python-style-fix: install-ruff
	ruff --version
	ruff format ${PYTHON_FILES}
	ruff check ${PYTHON_FILES} --fix
python-style-check: install-ruff
	ruff --version
	ruff format --check ${PYTHON_FILES}
	ruff check ${PYTHON_FILES}


SH_SCRIPTS   := $(shell grep -r -l --exclude='*~' --exclude='#*' --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)sh'   | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
BASH_SCRIPTS := $(shell grep -r -l --exclude='*~' --exclude='#*'  --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)bash' | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
CHECKBASHISMS := $(shell if command -v checkbashisms > /dev/null ; then \
	  echo "checkbashisms" ; \
	else \
	  wget -q -N https://homes.cs.washington.edu/~mernst/software/checkbashisms && \
	  mv checkbashisms .checkbashisms && \
	  chmod +x ./.checkbashisms && \
	  echo "./.checkbashisms" ; \
	fi)
shell-style-fix:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
	shfmt -w -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
	shellcheck -x -P SCRIPTDIR --format=diff ${SH_SCRIPTS} ${BASH_SCRIPTS} | patch -p1
endif
shell-style-check:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
	shfmt -d -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
	shellcheck -x -P SCRIPTDIR --format=gcc ${SH_SCRIPTS} ${BASH_SCRIPTS}
endif
ifneq ($(SH_SCRIPTS),)
	${CHECKBASHISMS} -l ${SH_SCRIPTS}
endif

showvars:
	@echo "PYTHON_FILES=${PYTHON_FILES}"
	@echo "SH_SCRIPTS=${SH_SCRIPTS}"
	@echo "BASH_SCRIPTS=${BASH_SCRIPTS}"
