JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')

.PHONY: all clean

# Delomboking seems to mess up line numbers. The actual error is on line 13, but the error appears on line 12.
# So check for both the error message and make sure it is for the right assignment.
# As of 2024-12-24, lombok does not work under Java 24, see https://projectlombok.org/changelog .
ifeq ($(shell test $(JAVA_VER) -gt 23; echo $$?),0)
all:
	@echo "Skipping test because lombok does not work under Java ${JAVA_VER}"
else
all:
	- ../../../gradlew build > Out.txt 2>&1
	(grep -qF "User.java:9: error: [argument.type.incompatible] incompatible argument for parameter y of FooBuilder.y." Out.txt \
	  && grep -qF "Foo.java:12: error: [assignment.type.incompatible] incompatible types in assignment." Out.txt \
	  && grep -qF "y = null; // error" Out.txt) \
	 || (echo "===== start of Out.txt =====" && cat Out.txt && echo "===== end of Out.txt =====" && false)
endif

clean:
	- ../../../gradlew clean
	rm -f Out.txt
