JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')
CHECKERFRAMEWORK ?= $(abspath ../../..)
export CHECKERFRAMEWORK

.PHONY: all clean

# The eisopcf Error Prone plugin requires JDK 21+.  On older JDKs, do nothing (succeed),
# mirroring how the framework-errorprone module is absent from the build below JDK 21.
#
# Uses -PcfVersion=local like the sibling examples: the example then consumes this checkout's
# jars.  Those jars are built by the :checker:exampleTests task (which depends on
# :framework-errorprone:jar and :checker:shadowJar), so no separate build step is needed here.
all:
ifeq ($(shell [ "$(JAVA_VER)" -ge 21 ] && echo yes),yes)
	- ../../../gradlew -PcfVersion=local build > Out.txt 2>&1
	grep -qF "Demo.java:23: warning: [eisopcf] [dereference.of.nullable]" Out.txt
else
	@echo "Skipping eisop-errorprone example: requires JDK 21+, found JDK $(JAVA_VER)."
endif

clean:
	- ../../../gradlew clean
	rm -f Out.txt
