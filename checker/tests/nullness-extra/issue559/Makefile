.PHONY: all clean

all:
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=myjdk.astub -AstubWarnIfOverwritesBytecode Issue559.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt

clean:
	rm -f Issue559.class Out.txt
