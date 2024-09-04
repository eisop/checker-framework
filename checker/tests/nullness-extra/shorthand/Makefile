.PHONY: all clean nullnessOnly nullnessRegex nullnessBad nonsense

all: nullnessOnly nullnessRegex nullnessBad nonsense

nullnessOnly:
	-$(JAVAC) -processor NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessOnlyExpected.txt Out.txt

nullnessRegex:
	-$(JAVAC) -processor NullnessChecker,RegexChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessRegexExpected.txt Out.txt

nullnessBad:
	-$(JAVAC) -processor nullness.NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessBadExpected.txt Out.txt

nonsense:
	-$(JAVAC) -processor NonsenseChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NonsenseExpected.txt Out.txt

clean:
	rm -f Out.txt
	rm -f *.class
