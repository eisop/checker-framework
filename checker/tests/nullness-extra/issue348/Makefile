.PHONY: all clean

all:
	mkdir -p bin
	$(JAVAC) lib/*.java -d bin/
	jar cvf lib.jar -C bin/ lib/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue348.java -d bin/ -cp lib.jar > Out.txt 2>&1
	diff -u Expected.txt Out.txt

clean:
	rm -f Out.txt lib.jar
	rmdir bin/
