JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//' | sed 's/-beta//')

.PHONY: all clean

ifeq ($(shell test $(JAVA_VER) -lt 17; echo $$?),0)
all:
	@echo "Skipping test because errorprone does not work under Java ${JAVA_VER}"
else
all:
	- ../../../gradlew build > Out.txt 2>&1
	grep -qF "Demo.java:7: warning: [CollectionIncompatibleType] Argument 'i - 1' should not be passed to this method; its type int is not compatible with its collection's type argument Short" Out.txt
	grep -qF "Demo.java:8: error: [argument.type.incompatible] incompatible argument for parameter arg0 of Set.add." Out.txt
endif

clean:
	- ../../../gradlew clean
	rm -f Out.txt
