.PHONY: all clean

all:
	bazelisk run example > Out.txt 2>&1 || true
	grep -qF "BazelExample.java:25: error: [assignment.type.incompatible] incompatible types in assignment." Out.txt || (echo "FAILURE.  Here is file Out.txt:" && cat Out.txt && echo "End of file Out.txt." && false)

clean:
	-bazelisk clean
	rm -f Out.txt
