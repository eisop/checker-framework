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

Question 1 has a first answer. Both tools run as Error Prone checks over the
same sources, in the same JVM, through the same javac, so the only variable is
which check runs.

**Method.** Modelled on NullAway's own JMH harness
(`jmh/src/main/java/com/uber/nullaway/jmh/NullawayJavac.java`): sources are read
into memory once, the analysis classes sit on the harness's classpath so they
are JIT-compiled rather than reloaded, and every configuration is warmed before
any is timed. Three rules keep it fair:

- **Identical javac flags for every configuration.** `-XDcompilePolicy=simple`,
  `--should-stop=ifError=FLOW` and `-XDaddTypeAnnotationsToSymbol=true` are
  passed even to the plain-`javac` baseline, which needs none of them.
- **Diagnostic caps raised** to 10^6. javac's defaults cap both errors and
  warnings at 100, which silently truncates any finding count above that.
- **Warm every configuration before timing any, then time round-robin**, so JIT
  and GC drift is spread across configurations instead of being charged to
  whichever ran first.

Corpus: Caffeine 3.0.2, 575 files -- one of NullAway's own benchmark corpora,
annotated with Checker Framework qualifiers, so neither tool reads a foreign
dialect. NullAway 0.13.6, Error Prone 2.50.0, eisop CF 3.49.5-eisop2-SNAPSHOT,
JDK 25. Median of 7 timed runs after 3 warmups.

| configuration | median | over EP baseline | warnings |
| --- | ---: | ---: | ---: |
| `javac`, no Error Prone | 0.96s | -- | 0 |
| Error Prone, all checks disabled | 0.98s | baseline | 0 |
| + NullAway, no annotated packages *(control)* | 0.98s | +0.00s | 0 |
| **+ NullAway** | **1.15s** | **+0.17s** | **0** |
| + `eisopcf`, nullness only | 6.60s | +5.62s | 2772 |
| + `eisopcf`, no Initialization | 10.58s | +9.60s | 2822 |
| + `eisopcf`, no Map Key | 14.79s | +13.81s | 3186 |
| **+ `eisopcf`, full Nullness Checker** | **18.94s** | **+17.96s** | **3236** |

Both tools were verified live on a shared control file -- a `@Nullable` field
dereferenced in the annotated package -- which each reports. The
`nullaway-nopkgs` row is the second control: NullAway loaded but with nothing to
analyse costs the same as no check at all, so the +0.17s above it is analysis
and not load time.

**The ratio is about 106x, and it is the wrong number to quote alone.** The two
tools did not do the same work. NullAway reported nothing on this corpus. The
Nullness Checker reported 3236 warnings, and in an earlier run with the caps
left at their defaults, **47 of the first 100 were
`type.argument.type.incompatible`** -- nullness *through generic type
arguments*, which is the area NullAway has historically declined to check. A
tool that checks more costs more. What the ratio does say is what a build feels,
because a build feels the whole compile.

Note also which corpus this is: Caffeine is *NullAway's* benchmark, so it is
plausibly kept NullAway-clean. A corpus chosen the other way would flatter the
other tool. That NullAway finds zero and the Nullness Checker finds 3236 is a
fact about this pairing, not a general ratio.

## Where the Nullness Checker's time goes

The Nullness Checker is three analyses. Attributing cost and findings to each,
against the full run:

| analysis | cost | share of analysis cost | findings | share of findings |
| --- | ---: | ---: | ---: | ---: |
| nullness core | 5.62s | 31% | 2772 | 86% |
| Initialization | 8.36s | 47% | 414 | 13% |
| Map Key | 4.15s | 23% | 50 | 1.5% |

The two subcheckers together are **69% of the analysis cost for 14% of the
findings**: about 27ms of analysis per finding, against 2ms per finding for the
nullness core -- **13x more expensive per finding**.

That is not an argument that they are worthless; a finding is not a unit of
value, and an initialization bug can matter more than ten nullness warnings. It
is an argument that a user who wants nullness checking is paying roughly 3x for
it by default, and has no obvious way to learn that.

PR #1998 is the lever that already exists: `-Amode=jspecify` sets
`-AassumeInitialized` and `-AassumeKeyFor` (and `-AonlyAnnotatedFor`). It was
proposed as a compatibility mode. On this evidence it is also the largest
single performance option the Nullness Checker has -- 18.94s to 6.60s here --
and neither its documentation nor its changelog entry says so.

## Two corrections

**An earlier version of this file claimed the two subcheckers "bought zero
findings".** That was an artifact of javac's default 100-diagnostic cap: every
Checker Framework configuration hit it, so they all reported the same truncated
count and looked identical. With the cap raised they differ, and the subcheckers
do find things -- 464 of them. The cost figures were unaffected; the findings
figures were wrong. Any measurement that reports a finding count of exactly 100
should be suspected of this.

**An earlier version also recorded plain `javac` as *slower* than Error Prone
with all checks disabled**, and waved at "compile policy and JIT ordering". That
was a real defect in the harness, not an oddity to note: the baseline was not
being given `-XDcompilePolicy=simple`, and each configuration was timed in a
fixed order, so the first one ran least warmed. With flags equalised and timing
round-robined, `javac` is 0.96s and Error Prone with no checks is 0.98s -- Error
Prone costs a little, as it must.

## What this does to the earlier hypothesis

An earlier measurement on this repository's own `framework/src/main/java`
(cold JVM, standalone processor) found the checker's cost over plain `javac`
stayed in a 1-2s band from 5 files to 275, and suggested the cost was close to
fixed. **That does not survive contact with third-party code.** On Caffeine the
cost is plainly proportional: +17.96s on 575 files, about 31ms per file, against
NullAway's 0.3ms. Running the same in-process harness on this repository's own
sources failed for an unrelated reason -- they need `--add-exports` at compile
time, which the harness does not pass -- so the two corpora have not been
measured under one harness and the discrepancy is not yet explained.

## Caveats

- **One corpus**, and it is the other tool's benchmark. Caffeine is
  generics-heavy cache code: the shape that most rewards the Checker
  Framework's extra checking and most punishes its cost.
- **The 3236 findings are unexamined.** Caffeine carries Checker Framework
  qualifiers but is not checked with the Nullness Checker in its own build, so
  these may be true positives, imprecision, or missing annotations. At 5.6
  findings per file, classifying even a sample would say a great deal -- and it
  belongs to [precision-and-adoption-cost.md](precision-and-adoption-cost.md),
  whose central question is exactly this number.
- Warm and in-process: JVM startup and one-time class loading are excluded.

## The three questions

1. ~~**On a real third-party codebase, what does the Checker Framework cost
   relative to plain `javac`, and how does that compare to NullAway on the same
   code?**~~ **Answered above, on one corpus: the analysis costs ~84x
   NullAway's, and 69% of that is two subcheckers producing 14% of the
   findings.** The reputation is supported, and it is an engineering problem,
   not a documentation one. The question that replaces it: **is a finding from
   the Initialization Checker worth thirteen times what a nullness finding
   costs?** That is a judgement about value, not a measurement, and it decides
   whether `-Amode=jspecify` should be advertised as the default way to run
   nullness checking or left as a compatibility mode.

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
