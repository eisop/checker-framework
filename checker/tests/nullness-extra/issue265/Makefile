.PHONY: all clean

all:
	mkdir -p bin
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker *.java -d bin/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Delta.java -cp bin/ -d bin/

clean:
	rm -rf bin
