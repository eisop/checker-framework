FILES=qual/Encrypted.java \
  qual/PossiblyUnencrypted.java

JAVAOPTS=

JAVAC?=../../../checker/bin/javac

# gets the full path to the directory of the make file, which is also the root dir of the qual folder
# for custom projects, it is best to encode the full root path as a variable
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
.PHONY: all clean compile-for-test named-quals-test qual-folder-test

all: compile-for-test named-quals-test qual-folder-test

demo:
	$(JAVAC) $(JAVAOPTS) $(FILES)
	@echo "***** This command is expected to produce a warning on line 12 and an error on line 35:"
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.common.subtyping.SubtypingChecker -AqualDirs=$(PROJECTDIR) Demo.java

# compile qualifiers
compile-for-test:
	$(JAVAC) $(JAVAOPTS) $(FILES)

# test case for using externally defined qualifiers by explicitly naming them using the -Aquals option
named-quals-test:
	-$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.common.subtyping.SubtypingChecker -Aquals=qual.Encrypted,qual.PossiblyUnencrypted Demo.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test case for using externally defined qualifiers by loading them from a directory using the -AqualDirs option
qual-folder-test:
	-$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.common.subtyping.SubtypingChecker -AqualDirs=$(PROJECTDIR) Demo.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test clean up
clean:
	rm -f qual/*.class
