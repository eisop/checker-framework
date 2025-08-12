JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')

.PHONY: all clean

ifeq ($(shell test $(JAVA_VER) -lt 17; echo $$?),0)
all:
	@echo "Skipping test because errorprone does not work under Java ${JAVA_VER}"
else
all:
	- ../../../gradlew build > Out.txt 2>&1
	# Check for crash due to mismatched Checker Qualifiers
	grep -qF "Exception: org.checkerframework.javacutil.BugInCF:" Out.txt
endif

clean:
	- ../../../gradlew clean
	rm -f Out.txt
