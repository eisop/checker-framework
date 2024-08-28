.PHONY: all

all: clean
	mkdir -p bin
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker *.java -d bin/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Delta.java -cp bin/ -d bin/

clean:
	rmdir bin
	rm bin/*.class
