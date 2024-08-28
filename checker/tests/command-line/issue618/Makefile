.PHONY: all

all: clean
	-$(JAVAC) -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java > out.txt 2>&1
	diff -u expected.txt out.txt

clean:
	rm -f out.txt
