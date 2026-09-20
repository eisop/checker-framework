JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')
CHECKERFRAMEWORK ?= $(abspath ../../..)
export CHECKERFRAMEWORK

.PHONY: all clean

all:
	- ../../../gradlew -PcfVersion=local build > Out.txt 2>&1
	(grep -qF "Demo.java:14: warning: [NullAway] passing @Nullable parameter 'null' where @NonNull is required" Out.txt \
	  && grep -qF "Demo.java:14: error: [argument.type.incompatible] incompatible argument for parameter name of Demo.generateGreeting." Out.txt) \
	|| (printf "FAILURE!\n===== start of Out.txt =====\n" && cat Out.txt && echo "===== end of Out.txt =====" && false)

clean:
	- ../../../gradlew clean
	rm -f Out.txt
