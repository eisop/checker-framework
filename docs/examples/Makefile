JAVAC ?= $(realpath ../../checker/bin/javac)

.PHONY: all clean

all: compile

compile:
	$(JAVAC) *.java
	cd fenum-extension && $(MAKE)
	cd subtyping-extension && $(MAKE)
	cd units-extension && $(MAKE)
# The Maven example downloads a lot.  Try twice in case of network lossage.
	cd MavenExample && ($(MAKE) || (sleep 60 && echo "Trying again:" && $(MAKE)))
	cd lombok && $(MAKE)
	cd errorprone && $(MAKE)
	cd BazelExample && $(MAKE)

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.

clean:
	rm -f *.class
	cd fenum-extension && $(MAKE) clean
	cd subtyping-extension && $(MAKE) clean
	cd units-extension && $(MAKE) clean
	cd MavenExample && $(MAKE) clean
	cd lombok && $(MAKE) clean
	cd errorprone && $(MAKE) clean
	cd BazelExample && $(MAKE) clean
