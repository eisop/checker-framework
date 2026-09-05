# Can the Checker Framework cost what NullAway costs?

## The problem

Apache Calcite, one of the larger public Checker Framework users, has an open
proposal to replace it ([CALCITE-7736], "Replace the Checker Framework with
NullAway and JSpecify"). The reasons it gives are not about what the Checker
Framework finds. They are about what it costs to run:

- "The Checker Framework is thorough but slow, which is why Calcite runs it in
  two CI jobs of its own rather than as part of an ordinary build." NullAway
  "is a single Error Prone check and costs a fraction of that, so nullness
  verification can move into a normal compile."
- The setup needs "a Gradle plugin of its own, 48 `.astub` files that patch the
  nullness of the JDK and of third-party libraries, and two dedicated CI jobs."
- NullAway "reports the whole set of problems in one pass," which makes a
  migration tractable.

The third-party stub problem is [library-models.md](library-models.md). This
file is about the first two: cost, and where in the build the check runs.

A checker that runs in a separate CI job is a checker whose findings arrive
after the code is written, reviewed, and merged. That is a different product
from one that runs in the compile the developer just started, no matter how
good the analysis is.

## What is already true

The gap may be smaller than the reputation. Two measurements:

- eisop's 3.49.5-eisop2 release notes record `checkNullness` at **1m28s versus
  3m40s** in the previous release, and `allNullnessTests` at 1m24s versus
  2m16s -- roughly 2.5x within one release cycle.
- On this repository's `framework/src/main/java` (275 files, fully annotated),
  wall clock for a cold `javac` was **4.8s** and for the same compile with
  `-processor nullness` **7.1s** -- about **1.5x**, measured 2026-09-05.

1.5x on an annotated corpus is not "run it in a separate CI job" territory. But
the full `checknullness` build tells a different story: a JFR trace of it
(10,059 on-CPU samples over 135s) attributes **83%** of on-CPU time to
`SourceChecker.typeProcess`. Both numbers are real; they differ in how much
plain `javac` work there is to amortize against, and in whether the annotated
JDK is loaded. Which one a user experiences is exactly what is unknown.

PR #1993 removes the second complaint's premise: the Checker Framework can now
run as an Error Prone check (`eisopcf`), on the same processorpath, in the same
compile, with no plugin of its own -- the integration NullAway has always had.
Nobody has measured what it costs there.

## The three questions

1. **On a real third-party codebase, what does the Checker Framework cost
   relative to plain `javac`, and how does that compare to NullAway on the same
   code?** Calcite itself is the obvious subject, since it is annotated for both
   and its maintainers have already formed a view. The answer is one number per
   tool on one corpus, and it either supports the reputation or refutes it. If
   it refutes it, the problem is documentation, not engineering, and the
   remaining two questions are moot.

2. **Where does the time go on a corpus that is *not* the Checker Framework's
   own source?** Every performance trace in `performance-notes.md` was taken on
   this repository checking itself. That corpus is unusual: fully annotated, no
   suppressions, written by people who know the analysis. A trace of Calcite or
   Guava may put the time somewhere the existing notes have never looked.

3. **What does `eisopcf` cost compared to the standalone processor, on the same
   code?** The Error Prone path shares the AST with every other check rather
   than driving its own compilation. That could be cheaper (one attribution,
   one traversal) or more expensive (Error Prone's own scanning on top). PR
   #1993 measured only its own overhead on synthetic finding-heavy files.

## First experiment

Check out Calcite at the commit named in CALCITE-7736. Build it three ways --
plain `javac`, eisop CF as a standalone processor, NullAway -- and record wall
clock and on-CPU samples for each. Publish the table whatever it says.

[CALCITE-7736]: http://www.mail-archive.com/dev@calcite.apache.org/msg26215.html
