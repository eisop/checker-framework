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

## Measured: NullAway and the Nullness Checker, same corpus, same compiler

Question 1 has a first answer. The measurement below runs both tools as Error
Prone checks over the same sources, in the same JVM, through the same javac --
so the only variable is the check.

**Method.** Modelled on NullAway's own JMH harness
(`jmh/src/main/java/com/uber/nullaway/jmh/NullawayJavac.java`): sources are read
into memory once, the analysis classes sit on the harness's classpath so they
are JIT-compiled rather than reloaded, and each configuration is warmed before
it is timed. Corpus: Caffeine 3.0.2, 575 files, which is one of NullAway's own
benchmark corpora and is annotated with Checker Framework qualifiers, so
neither tool is reading a foreign dialect. NullAway 0.13.6, Error Prone 2.50.0,
eisop CF 3.49.5-eisop2-SNAPSHOT, JDK 25. Median of 5 timed runs after 3 warmups.

| configuration | median | over EP baseline | findings |
| --- | ---: | ---: | ---: |
| `javac`, no Error Prone | 1.20s | -- | 0 |
| Error Prone, all checks disabled | 1.02s | baseline | 0 |
| + NullAway, no annotated packages *(control)* | 1.01s | +0.00s | 0 |
| **+ NullAway** | **1.24s** | **+0.22s** | **0** |
| + `eisopcf`, nullness only | 6.69s | +5.67s | 100 |
| **+ `eisopcf`, full Nullness Checker** | **19.27s** | **+18.25s** | **100** |

Both tools were verified live on the same control file -- a `@Nullable` field
dereferenced in the annotated package -- which each reports. The
`nullaway-nopkgs` row is the second control: NullAway loaded but with nothing to
analyse costs the same as no check at all, so the +0.22s in the row above is
real analysis and not load time.

**The headline number is ~84x, and it is the wrong number to quote alone.**
The two tools did not do the same work. NullAway reported nothing on this
corpus; the Nullness Checker reported 100 findings, and **47 of them are
`type.argument.type.incompatible`** -- nullness *through generic type
arguments*, which is exactly the area NullAway has historically declined to
check. A tool that checks more, costs more. What the number does say is what a
build feels, and a build feels the whole compile.

## Where the Nullness Checker's time goes: 65% of it bought nothing here

The Nullness Checker is three analyses. Turning the other two off, with the
findings unchanged at 100 in every row:

| configuration | median | share of full cost |
| --- | ---: | ---: |
| full Nullness Checker | 19.27s | -- |
| `-AassumeKeyFor` (no Map Key Checker) | 15.06s | -22% |
| `-AassumeInitialized` (no Initialization Checker) | 10.79s | -44% |
| both | 6.69s | **-65%** |

The two are nearly additive (4.21s + 8.48s = 12.69s against a measured 12.58s),
so they are independent costs rather than one masking the other.

On this corpus, two thirds of the Nullness Checker's cost produced no finding
that the nullness analysis did not already produce. That is not an argument
that those analyses are worthless -- they find real bugs, and a corpus where
they find nothing is a corpus that says nothing about corpora where they do. It
is an argument that **a user who wants nullness checking is currently paying 3x
for it by default**, and has no obvious way to know that.

PR #1998 is the lever that already exists: `-Amode=jspecify` sets
`-AassumeInitialized` and `-AassumeKeyFor` (and `-AonlyAnnotatedFor`). It was
proposed as a compatibility mode. On this evidence it is also the single
largest performance option the Nullness Checker has, and neither its
documentation nor its changelog entry says so.

## What this does to the earlier hypothesis

An earlier measurement on this repository's own `framework/src/main/java`
(cold JVM, standalone processor) found the checker's cost over plain `javac`
stayed in a 1-2s band from 5 files to 275, and suggested the cost was close to
fixed. **That does not survive contact with third-party code.** On Caffeine the
cost is plainly proportional -- +18.25s on 575 files, about 32ms per file,
against NullAway's 0.4ms. Attempting the same in-process comparison on this
repository's own sources failed for an unrelated reason (they need
`--add-exports` at compile time, which the harness does not pass), so the two
corpora have not been measured under one harness and the discrepancy is not
yet explained.

## Caveats

- **One corpus.** Caffeine is generics-heavy cache code, which is the shape
  that most favours the Checker Framework's extra checking and most disfavours
  its cost.
- **The 100 findings are unexamined.** Caffeine is annotated with Checker
  Framework qualifiers but is not checked with the Nullness Checker in its own
  build, so these may be true positives, imprecision, or missing annotations.
  Classifying them belongs to
  [precision-and-adoption-cost.md](precision-and-adoption-cost.md) and would
  make the cost comparison much more meaningful.
- `javac` at 1.20s is *slower* than Error Prone with all checks disabled at
  1.02s, an artifact of `-XDcompilePolicy=simple` and JIT ordering. The Error
  Prone row is used as the baseline throughout, which is the conservative
  choice: it makes the Checker Framework's overhead look larger, not smaller.
- Warm and in-process. JVM startup and one-time class loading are excluded.

Harness and raw output: not committed; regenerate from the method above.

## The three questions

1. ~~**On a real third-party codebase, what does the Checker Framework cost
   relative to plain `javac`, and how does that compare to NullAway on the same
   code?**~~ **Answered above, on one corpus: the analysis costs ~84x
   NullAway's, and about two thirds of that is two subcheckers that found
   nothing.** The reputation is supported, and it is an engineering problem, not
   a documentation one. The question that replaces it: **does the 65% hold on a
   corpus where the initialization and map-key analyses do find something?** If
   it does, `-Amode=jspecify` is the answer for most users and should be
   advertised as such. If it does not, the default is defensible and the work is
   elsewhere.

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
