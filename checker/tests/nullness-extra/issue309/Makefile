.PHONY: all clean

all:
	mkdir -p bin
	$(JAVAC) lib/*.java -d bin/
	jar cvf lib.jar -C bin/ lib/Lib.class
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue309.java -d bin/ -cp lib.jar > Out.txt 2>&1
	diff -u Expected.txt Out.txt
# Run again with stub file. (This used to cause a crash.)
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue309.java -d bin/ -cp lib.jar -Astubs=Lib.astub > Out.txt 2>&1
	diff -u Expected.txt Out.txt

clean:
	rm -f Out.txt lib.jar
	rm -rf bin/
