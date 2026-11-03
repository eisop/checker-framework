.PHONY: all clean

all:
	$(JAVAC) -processor tainting -Ashowchecks Issue1015.java > out.txt 2>&1
	diff -u --strip-trailing-cr expected.txt out.txt

clean:
	rm -f out.txt
	rm -f *.class
