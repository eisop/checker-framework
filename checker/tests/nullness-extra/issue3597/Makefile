.PHONY: all clean

all:
	$(JAVAC) testpkg/Issue3597B.java
	$(JAVAC) -Astubs=issue3597.astub -processor Nullness -sourcepath : -cp . testpkg/Issue3597A.java

clean:
	rm -f testpkg/*.class
