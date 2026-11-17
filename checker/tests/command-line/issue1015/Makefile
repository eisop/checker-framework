.PHONY: all clean

all:
	$(JAVAC) -processor tainting -Ashowchecks Issue1015.java > out.txt 2>&1
	tr -d '\r' < out.txt | diff -u expected.txt -

clean:
	rm -f out.txt
	rm -f *.class
