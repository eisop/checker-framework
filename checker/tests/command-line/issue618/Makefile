.PHONY: all clean

all:
	-${JAVAC} -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java 2>&1 | grep -v '^Picked up JAVA_TOOL_OPTIONS: ' > out.txt 2>&1
	tr -d '\r' < out.txt | diff -u expected.txt -

clean:
	rm -f out.txt
	rm -f *.class
