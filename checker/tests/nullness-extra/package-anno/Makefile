.PHONY: all clean

all:
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker test/*.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt

clean:
	rm -f Out.txt
	rm -f test/*.class
