.PHONY: all clean

all:
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Acfgviz=org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer,verbose -AassumeKeyFor CfgVizNodeValues.java > Out.txt 2>&1 || true
	diff -u Expected.out Out.txt

clean:
	rm -f *.class Out.txt
