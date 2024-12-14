JAVAC ?= $(realpath ../../checker/bin/javac)

.PHONY: all clean

all: compile

compile:
	$(JAVAC) *.java
	$(MAKE) -C fenum-extension
	$(MAKE) -C subtyping-extension
	$(MAKE) -C units-extension
	# The Maven example downloads a lot.  Try twice in case of network lossage.
	$(MAKE) -C MavenExample || (sleep 60 && echo "Trying again:" && $(MAKE) -C MavenExample)
	$(MAKE) -C lombok
	$(MAKE) -C errorprone
	$(MAKE) -C BazelExample

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.

clean:
	rm -f *.class
	$(MAKE) -C fenum-extension clean
	$(MAKE) -C subtyping-extension clean
	$(MAKE) -C units-extension clean
	$(MAKE) -C MavenExample clean
	$(MAKE) -C lombok clean
	$(MAKE) -C errorprone clean
	$(MAKE) -C BazelExample clean
