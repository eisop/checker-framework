.PHONY: all

all: nullnessOnly nullnessRegex nullnessBad nonsense

nullnessOnly: clean
	-$(JAVAC) -processor NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessOnlyExpected.txt Out.txt

nullnessRegex: clean
	-$(JAVAC) -processor NullnessChecker,RegexChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessRegexExpected.txt Out.txt

nullnessBad: clean
	-$(JAVAC) -processor nullness.NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NullnessBadExpected.txt Out.txt

nonsense: clean
	-$(JAVAC) -processor NonsenseChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u NonsenseExpected.txt Out.txt

clean:
	rm -f Out.txt
