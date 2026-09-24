JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')
CHECKERFRAMEWORK ?= $(abspath ../../..)
export CHECKERFRAMEWORK

.PHONY: all clean

all:
	- ../../../gradlew -PcfVersion=local build > Out.txt 2>&1
	# 1. Verify that EISOP Checker Framework ran
	grep -qF "Note: Checker Framework" Out.txt
	# 2. Verify that typetools checker-qual was detected (missing applyToSubpackages emitted NOTE)
	grep -qF "The @DefaultQualifier annotation on the classpath does not define the applyToSubpackages element; package defaults will apply to subpackages." Out.txt
	# 3. Verify no crash occurred (BugInCF)
	! grep -qF "Exception: org.checkerframework.javacutil.BugInCF:" Out.txt
	# 4. Verify that type checking ran and enforced the package @DefaultQualifier
	grep -qF "error: [argument.type.incompatible] incompatible argument for parameter s of Demo.acceptInterned." Out.txt

clean:
	- ../../../gradlew clean
	rm -f Out.txt
