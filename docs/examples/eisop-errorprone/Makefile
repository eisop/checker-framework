JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')
CHECKERFRAMEWORK ?= $(abspath ../../..)
export CHECKERFRAMEWORK

.PHONY: all build-cf-jars clean

# The eisopcf Error Prone plugin requires JDK 21+.  On older JDKs, do nothing (succeed),
# mirroring how the framework-errorprone module is absent from the build below JDK 21.
all:
ifeq ($(shell [ "$(JAVA_VER)" -ge 21 ] && echo yes),yes)
	$(MAKE) build-cf-jars
	- ../../../gradlew build > Out.txt 2>&1
	grep -qF "Demo.java:23: warning: [eisopcf] [dereference.of.nullable]" Out.txt
else
	@echo "Skipping eisop-errorprone example: requires JDK 21+, found JDK $(JAVA_VER)."
endif

# Build the locally-produced jars the example consumes: checker-qual, the eisopcf plugin, and the
# checker "-all" shadow jar (which bundles the checkers).
build-cf-jars:
	../../../gradlew -p ../../.. :checker-qual:jar :framework-errorprone:jar :checker:shadowJar

clean:
	- ../../../gradlew clean
	rm -f Out.txt
