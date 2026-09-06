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

Both tools run as Error Prone checks over the same sources, in the same JVM,
through the same javac, so the only variable is which check runs.

**Method.** Modelled on NullAway's own JMH harness
(`jmh/src/main/java/com/uber/nullaway/jmh/NullawayJavac.java`): sources are read
into memory once, the analysis classes sit on the harness's classpath so they
are JIT-compiled rather than reloaded, and every configuration is warmed before
any is timed. Every configuration gets identical javac flags, including the
plain-`javac` baseline; diagnostic caps are raised to 10^6, because javac caps
*warnings* at 100 by default as well as errors; and timing is round-robin after
warming all configurations, so JIT drift is not charged to whichever ran first.

Corpus: Caffeine 3.0.2, 575 files -- one of NullAway's own benchmark corpora,
annotated with Checker Framework qualifiers. **With `@SuppressWarnings("NullAway")`
removed; see below.** NullAway 0.13.6, Error Prone 2.50.0, eisop CF
3.49.5-eisop2-SNAPSHOT, JDK 25. Median of 5 timed runs after 2 warmups.

| configuration | median | over EP baseline | warnings |
| --- | ---: | ---: | ---: |
| `javac`, no Error Prone | 0.97s | -- | 0 |
| Error Prone, all checks disabled | 0.99s | baseline | 0 |
| + NullAway, no annotated packages *(control)* | 1.00s | +0.01s | 0 |
| **+ NullAway** | **1.53s** | **+0.54s** | **670** |
| + `eisopcf`, nullness only | 6.66s | +5.67s | 2772 |
| + `eisopcf`, no Initialization | 10.66s | +9.67s | 2822 |
| + `eisopcf`, no Map Key | 15.05s | +14.06s | 3186 |
| **+ `eisopcf`, full Nullness Checker** | **19.44s** | **+18.45s** | **3236** |

**The analysis cost ratio is about 34x.**

### The corpus suppresses NullAway, and that changed everything

**537 of Caffeine's 575 files -- 88% of its lines -- carry
`@SuppressWarnings({"unchecked", "MissingOverride", "NullAway"})`.** They are
Caffeine's generated cache classes, and the project excludes them from NullAway
by hand. The Checker Framework does not honour that key, so it checked code
NullAway had been told to skip.

Measured before this was noticed, NullAway reported **0** findings and cost
**+0.17s**, and the ratio read as **106x**. Removing only the `"NullAway"` key
from those suppressions -- leaving the code otherwise untouched -- takes
NullAway to **670** findings and **+0.54s**, and the ratio to **34x**. The
Checker Framework's numbers are unchanged either way, which is the consistency
check that the edit did what it claims.

The first reading was not a small error. It was measuring one tool on 12% of
the corpus and the other on all of it.

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

## What the extra findings are: 77% of them are generics

With both tools now analysing the whole corpus, the two finding sets can be
compared by source location. Full Nullness Checker, distinct locations:

| | locations | share |
| --- | ---: | ---: |
| generics-involved | 1303 | 77% |
| not generics-involved | 379 | 23% |

"Generics-involved" means the message key is about type arguments or bounds, or
the reported `found`/`required` types contain a type argument or a type-variable
use. Of the 379 that are not, **213 are initialization** findings -- which
NullAway also checks, and which are its second-largest category. That leaves
**166 findings that are neither about generics nor about initialization**, out
of 1682 locations: **about 10%**.

Running the Checker Framework with nullness only (`-AassumeInitialized
-AassumeKeyFor`) gives the same picture from the other side: 146 non-generic
locations out of 1353, **11%**.

### Where the two tools agree and disagree

| | locations |
| --- | ---: |
| reported by both | 220 |
| NullAway finding at a location the CF also flags, but classifies as generic | 299 |
| **NullAway only** -- the CF is silent | **149** |
| **CF non-generic only** -- NullAway is silent | **159** |

519 of NullAway's 668 locations (78%) have a Checker Framework finding on the
same line. The two disagreement sets are the interesting ones, and they point in
opposite directions: 149 places where NullAway may be catching something the
Checker Framework does not, and 159 where the reverse might hold.

### Are the 159 NullAway unsoundness? Mostly not

Classifying the 167 non-generic, non-initialization findings that NullAway is
silent on, by the types in the message:

| | count | share |
| --- | ---: | ---: |
| literal `null` passed where the CF's library annotations say `@NonNull` | 105 | 63% |
| `@Nullable X` where `@NonNull X` required (same base type) | 20 | 12% |
| other typed mismatch | 19 | 11% |
| no types in the message (dereference, annotation-location, ...) | 18 | 11% |
| initialization qualifier | 5 | 3% |

Two samples, checked in the source:

- `LocalAsyncCache.java:287` -- `super("null map", null)`, calling
  `CompletionException(String, Throwable)`. The JDK permits a null cause. This
  is the Checker Framework's annotated JDK disagreeing with NullAway's library
  model, not a bug either tool missed. It is the largest category, at 63%.
- `Caffeine.java:1184` -- `if (keyStrength != null) { s.append("keyStrength=")
  .append(keyStrength.toString()...) }`. The field is null-checked immediately
  above the dereference, but the intervening `append` call is not
  `@SideEffectFree`, so the Checker Framework discards the refinement. A false
  positive. `Caffeine.java:340` is the same pattern.

**On the evidence sampled, the non-generic surplus is dominated by library-model
differences and by the Checker Framework's own imprecision, not by NullAway
missing bugs.** That is a sample, not a census: only a handful of the 167 were
read in full, and the 149 NullAway-only locations were not examined at all.
Classifying both disagreement sets properly is
[precision-and-adoption-cost.md](precision-and-adoption-cost.md)'s central
question, and this corpus is now a ready-made subject for it.

## Two corrections to earlier versions of this file

**"The two subcheckers bought zero findings."** An artifact of javac's default
100-diagnostic cap, which applies to warnings as well as errors: every Checker
Framework configuration hit it and reported the same truncated number. With the
cap raised they differ. Any measurement reporting exactly 100 diagnostics should
be suspected of this.

**Plain `javac` measured slower than Error Prone with all checks disabled.** A
harness defect, not an oddity: the baseline was not getting
`-XDcompilePolicy=simple`, and configurations were timed in a fixed order so the
first ran least warmed. With flags equalised and timing round-robined, `javac`
is 0.97s and Error Prone with no checks is 0.99s.

**And the third, above: the corpus suppressed NullAway on 88% of its lines.**
All three were found by review of the result rather than by the harness, which
is the lesson worth keeping: every one of them made the Checker Framework look
worse or the comparison look cleaner than it was.

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
  Framework's extra checking and most punishes its cost. It is also *NullAway's*
  benchmark, and 88% of it is generated code the project excludes from NullAway,
  which had to be undone to compare anything.
- **The findings are only partly examined.** They were classified
  mechanically, by message key and by the types printed in the message; only a
  handful were read in the source. The mechanical split is reliable for
  "generics or not" and much weaker for "true positive or not".
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
