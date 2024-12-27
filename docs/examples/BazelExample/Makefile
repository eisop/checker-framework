JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')

.PHONY: all clean

ifeq ($(shell test $(JAVA_VER) -gt 21; echo $$?),0)
all:
	@echo "Skipping test because I don't know how to configure bazel to work on a JDK > 21..."
else
all:
	bazelisk run example > Out.txt 2>&1 || true
	grep -qF "BazelExample.java:25: error: [assignment.type.incompatible] incompatible types in assignment." Out.txt || (echo "FAILURE.  Here is file Out.txt:" && cat Out.txt && echo "End of file Out.txt." && false)
endif

clean:
	-bazelisk clean
	rm -f Out.txt
