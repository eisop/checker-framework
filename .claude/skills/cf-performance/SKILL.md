---
name: cf-performance
description: Use when profiling or optimizing the EISOP Checker Framework. Triggers on any mention of JFR, async-profiler, hotspots, "slow checker", allocations, GC pressure, "the framework is taking too long", or proposing performance patches against framework, dataflow, javacutil, checker, or common code. Codifies the capture procedure, analysis pipeline, and known-already-optimized hotspots.
---

# Performance work on the EISOP Checker Framework

Use this skill before proposing any performance change. The cost of
re-proposing an already-applied optimization, or proposing one based on
reading code rather than samples, is a wasted review cycle. The cost of
reading this skill first is small.

## Step 0: Check what has already been done

Before profiling, before reading code, before proposing anything, scan:

- [`docs/developer/performance-notes.md`](../../../docs/developer/performance-notes.md)
  — the canonical log of applied and rejected perf work.
- `git log --grep=cache --grep=hashCode --grep=allocation --grep=boxing --grep="hot path" --since="6 months ago"`
- `git log --grep="Review of"` — package-by-package review PRs.

Optimizations that are commonly re-proposed and **have already been
applied**: replacing `TreeSet` with `ArrayList` in `AnnotationMirrorSet`;
caching `AnnotationMirrorSet#hashCode` with a dirty flag; pre-sizing
`AnnotatedTypeScanner.visitedNodes` and clearing rather than reallocating
on reset; caching `Name.toString()` in `ElementUtils.getQualifiedNameString`;
eliminating `Integer` boxing and lambda dispatch in `HashcodeAtmVisitor`;
adding a `syncedFrom` short-circuit in `AbstractAnalysis.setNodeValues`;
empty-map short-circuit in `AnnotationUtils.sameElementValues`; dropping
the `Collections.synchronizedMap` wrapper around `annotationClassNames` in
`AnnotatedTypeFactory`; eliminating `containsKey`+`get` double-lookups on
LRU caches; `Long`→`long` in `BaseTypeVisitor.checkSlowTypechecking`;
static `EnumSet` for `validateWildCardTargetLocation`. See the notes file
for the full list and rationale.

## Profile the whole build, not one subproject

**The realistic workload is the unqualified `./gradlew checknullness`, not
`:checker:checkNullness`.** The unqualified task runs the checker over **all
~10 subprojects** (checker, checker-qual{,-android}, checker-util, dataflow,
docs, framework, framework-perf, framework-test, javacutil). Gradle routes
every one of those forked compiles through a **single persistent
compiler-worker JVM**, so the full build still yields one dominant worker file
— but that file now contains all ten compiles: **~15–17k `ExecutionSample`s /
~155–170 s on-CPU**. A qualified `:checker:checkNullness` captures **one**
subproject (~2.6k samples / ~26 s) and undercounts build-level impact by
roughly 2×: an optimization that looks like "−22%" on that slice can be ~−9%
on the build. (This bit once — a cache reported as −22%/−26% on the
`:checker:checkNullness` slice was ~−9% cold / ~−13% warm-daemon end-to-end;
see the `performance-notes.md` full-build A/B table.)

**Always report the whole-worker sample delta + wall clock as the headline,
never one phase's inclusive %.** The `phase` mode of the analyzer prints the
recording's wall span and fires a `*** SCOPE WARNING ***` when a trace has
< 6000 `ExecutionSample`s — i.e. when you've profiled a slice. Heed it.

For a clean A/B, measure both sides identically — build the processor
`shadowJar` on each side (the forked javac uses it as the annotation
processor, so framework changes only take effect after a rebuild), and use the
same `--no-daemon` invocation (a warm daemon shaves per-fork JVM startup and
shifts the wall-clock baseline).

## Capturing a clean JFR trace

A naïve `./gradlew test` JFR capture **will be corrupted**: every JVM the
build spawns (the Gradle launcher, the daemon, the worker, any forked
javac) inherits `JAVA_TOOL_OPTIONS` and, if they share one `filename=`,
they clobber each other and shred the constant pool — the trace comes back
with `<null>` thread names and is dominated by the launcher's idle frames
(`sun.nio.ch.EPoll.wait`, `ProcessHandleImpl.waitForProcessExit0`) instead
of the type-checking work. The capture script handles this by giving each
JVM its own per-PID file (the JFR `%p` filename token); use it.

For the full realistic build, run the unqualified task `--no-daemon` so the
gradle JVM (and the forks it spawns) inherit the JFR env directly:

```
./checker/bin-devel/record-jfr.sh -o cf.jfr -- \
    ./gradlew --no-daemon checknullness
```

This writes `cf-<pid>.jfr` for each JVM. **The type-checking worker is by far
the largest file** (the others — gradle JVM, idle helpers — are tens to
hundreds of KB; the worker is 100s of MB). Pass all of them to the analyzer
(below); the small files contribute almost no `ExecutionSample`s. Sanity-check
the `phase` output: a full build is ~15k+ samples / ~155 s+ span and must NOT
print the scope warning. (`outputs.upToDateWhen { false }` makes these tasks
always re-run, so no `--rerun` is needed.)

To profile a *single* subproject deliberately — e.g. to iterate fast on a
checker-specific hot path — use `:checker:NullnessTest --tests "..."
-PmaxParallelForks=1`; with a `Test` task, Gradle's `forkEvery` may restart
the worker, splitting work across two or three large files — aggregate them.
But validate the final win on the full `checknullness`, not the slice.

Or for direct `javac` invocations on a real downstream project (Oscar EMR,
JSpecify reference checker, etc.), use the script's `--javac` mode:

```
./checker/bin-devel/record-jfr.sh -o cf.jfr --javac -- \
    ./checker/bin/javac -processor nullness -d /tmp/out src/**/*.java
```

For Maven projects with a forked compiler, put the JFR flags inside
`<compilerArgs>` with `-J` prefixes and `<fork>true</fork>`. The
`-Dmaven.compiler.compilerArgs` system property (plural) is silently
ignored. The file path should be absolute (`${project.build.directory}/oscar.jfr`),
not relative — multi-module Maven runs javac in each module's directory.

JFR's method sampler has a documented floor of `MIN_SAMPLE_PERIOD = 10ms`
in `jfrThreadSampler.cpp`. Values below 10ms are silently clamped, so
asking for 5ms wastes the configuration; it stays at 10ms.

## Analyzing the trace

**Do not use `jfr view` or `jfr print`.** On JDK 25 both crash with a
`StringIndexOutOfBoundsException` in `ValueFormatter.formatMethod` /
`PrettyWriter.formatMethod` on some mangled method signatures — not just
occasionally, but on the first frame they hit, so `jfr view hot-methods`,
`jfr view allocation-by-class`, and `jfr print --json` are all unusable.
`jfr summary` still works (it prints no method names).

Use the in-repo analyzer, which reads the recording directly via
`jdk.jfr.consumer.RecordingFile` and bypasses the broken formatter:

```
# Top self-time, total-time, and allocation-by-class (pass all per-PID files):
java .claude/skills/cf-performance/jfr-analyze.java top cf-*.jfr

# Attribute a hot JDK leaf back to the nearest CF frame:
java .claude/skills/cf-performance/jfr-analyze.java self java.util.HashMap.getNode \
    org.checkerframework cf-*.jfr

# Attribute allocations of a class to the nearest CF frame:
java .claude/skills/cf-performance/jfr-analyze.java alloc '[Ljava.lang.Object;' \
    org.checkerframework cf-*.jfr
java .claude/skills/cf-performance/jfr-analyze.java alloc 'ArrayList$Itr' \
    org.checkerframework cf-*.jfr
```

For *architectural* work (when the leaf profile is flat), the inclusive /
context modes are what make progress — use them, not leaf self-time:

```
# High-level wall-clock breakdown: on-CPU Java by CF subsystem, + GC + native idle.
# Start here for "where does checkNullness spend time". Prints the recording wall
# span and a *** SCOPE WARNING *** if the trace is a single-subproject slice
# (< 6000 samples) rather than the full ~15k-sample build.
java .claude/skills/cf-performance/jfr-analyze.java phase cf-*.jfr

# Inclusive time: which high-level operation dominates (it nests, unlike self-time).
java .claude/skills/cf-performance/jfr-analyze.java inclusive org.checkerframework cf-*.jfr

# Where one subsystem spends its self-time:
java .claude/skills/cf-performance/jfr-analyze.java under performFlowAnalysis cf-*.jfr

# Attribute a cost to a calling context (e.g. is this mostly under stub parsing?):
java .claude/skills/cf-performance/jfr-analyze.java cooccur getDeclAnnotation AnnotationFileParser cf-*.jfr

# Retained (post-GC live) heap from jdk.GCHeapSummary "After GC" events — for memory A/B
# (e.g. does a new cache increase live heap?). Report max + p90 + median; the master-vs-branch
# delta is the added retained footprint. (profile-cf.jfc already captures GCHeapSummary.)
java .claude/skills/cf-performance/jfr-analyze.java heap cf-*.jfr
```

Self-time is computed from `jdk.ExecutionSample` ONLY. Including
`jdk.NativeMethodSample` pollutes the leaderboard with idle native frames
(`EPoll.wait` on the Gradle worker's messaging thread can be >50% of the
combined samples). `getThread()` is `null` in these traces, so per-thread
filtering does not work — but the compilation is single-threaded, so every
`ExecutionSample` is real work and no filtering is needed.

## Measuring allocation deterministically (when TLAB sampling is too noisy)

JFR's allocation events (`jdk.ObjectAllocationSample`, TLAB) are **sampled**: two
identical runs of these workloads vary ~2× in per-class allocation counts (real example:
`TreePath` read 3060 vs. 1376 samples across two identical runs), which buries any sub-2%
allocation change. The `alloc`/`top` modes above are for **attribution** (which CF frame
allocates a class), *not* for the magnitude of a small change.

For magnitude, read `jdk.ThreadAllocationStatistics` instead — it reports each thread's
**cumulative** bytes allocated (not sampled), so a deterministic single-process workload
reproduces to ~0.15%. Run one forked `javac` over a fixed source set and read it back:

```
checker/bin/javac \
  -J-XX:StartFlightRecording=settings=profile,filename=run.jfr,dumponexit=true \
  -processor nullness -d /tmp/out  Foo.java ...
java .claude/skills/cf-performance/alloc-total.java run.jfr
```

Median of ≥3 runs/side; a real change clears the ~0.5% run-to-run band. This is the
allocation analogue of the wall-clock A/B below and caught a −4% change TLAB sampling
could not resolve. (`-J` forwards the flag to the forked compiler JVM — `checker/bin/javac`
is `CheckerMain`, which forks. Use a single source set so there is one compile thread.)
It measures exactly the checker + javac allocation, independent of GC/JIT scheduling.

`ab-measure.sh` automates this for one build side — it runs each given source file REPS
times and prints the median allocation **and** median wall (in separate runs, since JFR
perturbs timing):

```
git checkout <baseline>  && ./gradlew assembleForJavac && \
    .claude/skills/cf-performance/ab-measure.sh -l master Big300.java Varargs.java
git checkout <treatment> && ./gradlew assembleForJavac && \
    .claude/skills/cf-performance/ab-measure.sh -l branch Big300.java Varargs.java
```

Two cautions about this deterministic-allocation measurement:

- **A `settings=profile` trace does NOT carry TLAB / `ObjectAllocationSample` events**, so
  `jfr-analyze.java top`/`alloc` on it report `TLAB events: 0` and attribute nothing. Use
  these single-`javac` traces only for *magnitude* (`alloc-total.java` reads
  `ThreadAllocationStatistics`) and self-time; for allocation-*by-class attribution* capture
  with `record-jfr.sh`'s JFC instead.
- **It is blind to pure-CPU / traversal wins.** A change that does fewer scans / less work but
  allocates the same shows up flat here (the shallow-location defaulting shortcut cut scan
  calls 10% with *flat* allocation). Classify your change first: for an allocation change use
  this; for a CPU/traversal change measure wall and/or on-CPU `ExecutionSample` counts, or
  instrument an operation counter (below).

## Detecting super-linear (quadratic) costs with a size sweep

The all-systems corpus is 267 **tiny** files (1–3 method bodies each): a good
getPath / per-file-fixed-cost stressor, but **blind to any cost that is super-linear in a
per-compilation-unit dimension** — those wash out at small N. The per-body `Trees.getPath`
search in CFG construction (PR #1786) was **quadratic in methods-per-file** yet measured
**~0% on all-systems**; only a size sweep exposed it:

| methods/file | master | after | reduction |
| --- | --- | --- | --- |
| 100  |    524 MB |    506 MB |  −3.5% |
| 600  |  3525 MB |  2737 MB | −22.4% |
| 1500 | 15192 MB | 10193 MB | −32.9% |

Generate the inputs and sweep with the deterministic reader:

```
for n in 100 300 600 1500; do
  .claude/skills/cf-performance/gen-sized-program.py $n > /tmp/Big$n.java
done
# A/B each Big$n.java per side (allocation reader above); plot allocation/wall vs. n.
```

A curve that is super-linear on master and flattens after the change is the quadratic's
signature. **Report both ends:** small-N is the realistic per-file cost (typically
low-single-digit), large-N is worst-case protection (machine-generated / giant
single-class files). The generator's `--shape` flag selects which machinery the body
stresses (`generic` default, `vararg`, `deep-nesting`, `many-fields`) — e.g. `--shape
vararg` stresses `AnnotatedExecutableType` copying and is what exposed PR #1798's copier
vararg-aliasing bug; add a shape there for other mechanisms (large `switch`, etc.).

## Measuring wall-clock effects (the A/B that decides if a change is worth it)

JFR self-time / `phase` percentages are for **mechanism** ("which leaf
dropped, which subsystem shrank"). They are *not* a reliable read of the
**end-to-end** effect — a clear −15% in one phase can be a real ≈10% on the
build or lost in noise. For the number that decides whether to ship, measure
**wall clock**, and measure it the way it actually gets run. Hard-won rules:

- **Use the daemon (warm), not `--no-daemon`.** A single `--no-daemon` run
  *undercounts*: cold per-fork JVM startup dilutes the type-checking gain, and
  one run is noise-dominated. (A real example: an element-type cache read as
  "≈2%/noise" from one `--no-daemon` run measured **≈10%** under warm-daemon
  reps — 2m34s → 2m19s.) Run `./gradlew checknullness` (no flags) as the user does.
- **Rebuild the processor `shadowJar` on each side** (the forked javac uses it
  as the annotation processor, so framework changes only take effect after
  `:checker:shadowJar`). Stash exactly the runtime files to flip sides.
- **Warm, then take a median of ≥3 reps/side.** Discard the first 1–2 runs
  after a `shadowJar` change (the persistent compiler-worker JVM JIT is cold);
  read Gradle's own "BUILD SUCCESSFUL in Xm Ys". Watch for a downward warming
  trend — if reps are still dropping, warm more. Tightly clustered reps (e.g.
  2m19s ×4) are the signal; a 5–10 s spread is normal noise.
- **No JFR / `JAVA_TOOL_OPTIONS` during wall-clock reps** — recording perturbs
  timing. Do mechanism (JFR) and wall-clock (plain) in separate runs.
- **NEVER A/B two changes at once.** They can cancel and read as "no effect."
  (Real example: Phase 1 (≈ −10%) measured together with a `directSupertypes`
  cache-shrink (≈ +10%) netted *zero* wall-clock change; isolating each showed
  both effects clearly.) One variable per A/B.
- **Cross-session warmth is a confound.** Baseline and treatment measured in
  separate daemon sessions can drift; if the gap is small, interleave
  A/B/A/B/A/B. If it's large and the treatment reps are tightly clustered well
  outside the baseline spread, that's usually conclusive enough.

For **retained-memory** A/B (does a cache grow live heap?), use the `heap`
mode above on a traced run of each side; the post-GC-live-heap delta is the
added footprint. Caveat learned: shrinking a cache to reclaim that memory is
**not free** — a `directSupertypes` cap halving recovered ~half its footprint
but cost ≈10% wall clock (far more than its hit-rate delta implied). Cut
*per-entry weight*, not entry count, when memory matters.

## Two habits that paid off

- **Size a risky change before building it — instrument and *simulate*.** Before
  rewriting a hot path, add pure-counting instrumentation that *also* simulates the
  proposed variant's metric, changing no behavior. A counter on the eager `new TreePath`
  in `CFGTranslationPhaseOne.scan` that *also* simulated a lazy-stack alternative showed
  the lazy version would save 47.9% of only **0.56%** of `TreePath` allocation (~0.01% of
  total) — rejected in minutes, no risky rewrite of dataflow's central traversal. The JFR
  `alloc` nearest-CF attribution is what tells you which line to instrument (here it
  separated the per-tree `scan`, 0.56%, from the body-path `process`, 70%).
- **Re-measure marginal or "rejected" optimizations after a related change lands.** An
  optimization's value scales with the **traffic** through the code it touches, and another
  change can amplify it. A lazy `TreePathCacher` was ~1–4% standalone, but layered on
  PR #1786 (which routes one `getPath` per body through the cacher) it was **−51%** on a
  1500-method file — synergy, not additivity. A "not worth it" verdict is not permanent
  across an evolving hot path; the *Tried and rejected* section is a starting point, not a
  closed book.
- **Re-trace the current baseline before committing to a multi-PR plan — a logged hotspot may
  already be gone.** The same traffic-changes-value effect runs in reverse: an optimization
  named as "the recommended next direction" in `performance-notes.md` can be overtaken by
  intervening commits. The PR #1798 immutability program was sized off a logged
  "`AnnotatedTypeCopier` ≈ 2% self-time / ~22% of `Object[]` allocation" figure; a fresh
  full-`checknullness` trace *at the start of the work* would have shown it was already
  ~0.76% / ~1.5% — earlier caches (PR #1777), the thread-local copier map, and lazy
  `visitedNodes` had harvested it — so the boundary flips had little left to remove and came in
  at ~1%, not the projected large win. Profile **today's** code before planning, not the
  number in the log. Corollary: **never repeat a prior-session A/B number — re-measure against
  the current baseline.** A/B deltas do not compose; the −5.3% from one session did not
  reproduce once an earlier flip had already shipped (the baseline moved under it).
- **Measure the *addressable fraction* before building anything that helps only a subset.**
  If a change only helps when some condition holds (the type is frozen, the consumer is
  read-only, the default is a top-level location), first count how often that condition is
  actually true on a real workload — the cheapest possible experiment. Several PR #1798
  dead-ends died in minutes this way: caching `hashCode` on frozen types (**0%** of
  `hashCode` calls hit frozen types — all hot hash targets are mutable copies); the
  shallow-location defaulting shortcut (only **10%** of scans, over cheap types). The recipe:
  a `static AtomicLong` counter + a JVM shutdown hook that prints to stderr, and a
  `-Dflag`-gated toggle so you can A/B both variants in *one* build (no rebuild). This is the
  no-behavior-change cousin of "instrument and simulate" above.
- **Count ≠ cost (companion to "hit-rate ≠ win").** Reducing the *number* of operations is
  not a win if the skipped operations are cheap. The shallow defaulting shortcut cut scan
  *calls* 10% but allocation was flat, because the skipped scans were over cheap `Object`
  types; the expensive scans (deep `OTHERWISE`/bound traversals over generic types) were
  untouched. Measure the cost of the work you remove, not its count.
- **A flat A/B can mean the workload never reached your code — confirm the path is hit.**
  Before trusting a null result, check the change is actually exercised on that workload. Two
  PR #1798 A/Bs read flat for exactly this reason: a non-generic-call program showed nothing
  for the `elementTypeCache` flip because non-generic calls hit `methodAsMemberOfCache`, which
  short-circuits *before* `elementTypeCache` is consulted; a generic-varargs program was
  dominated by type-argument inference (which copies regardless), drowning the flip. The cheap
  guard is a counter on the code you changed — if it never fires, "no effect" is a workload
  bug, not a verdict on the change. Pair this with picking a workload that *maximizes* traffic
  through the target (the size-sweep / shape generators exist for this).

## Removing a defensive copy (the opposite of adding a cache)

Most of this skill is about *adding* a cache to save recomputation. *Removing* a defensive
copy (e.g. making a cache return a shared immutable value instead of a fresh `deepCopy()`)
is a distinct kind of work with its own failure mode, learned the hard way in PR #1798.

- **A boundary flip pays only if the cache's *hot* consumer is read-only — and you cannot
  tell that from the freeze flush.** A cache that hands out `deepCopy()` on every hit does so
  because *some* consumer mutates the result. The question is not *whether* a consumer mutates
  but whether the **dominant** one does. If the hot consumer rewrites the whole result — the
  tree pipeline (defaulting + flow refinement + annotators), type-argument inference, or
  `asSuper` — the flip just **moves** the copy to that consumer and is a wash (PR #1798:
  `elementCache`-into-the-tree-pipeline, the `methodFromUse`/inference copy-elision, and the
  `directSupertypesCache`/`AsSuperVisitor` flip all relocated the copy). But if the hot
  consumers are mostly read-only, the flip removes a real copy: the `elementTypeCache` and
  `classAndMethodTreeCache` flips shipped this way — though only **~1%**, because by then the
  copier was already cheap (see "re-trace before planning"). **The trap:** the freeze flush
  (below) enumerates only the *mutating* consumers — it is structurally blind to the read-only
  majority, so reasoning "look at all these mutators, the flip won't pay" is survivorship bias.
  Reasoning from the flush is how I first wrongly called these flips a wash. **Measure the
  read-only fraction directly** (a counter at the cache-return: how often is the result mutated
  before the next cache call?), don't infer it from who shows up in the flush.
- **Freeze-as-bug-finder.** To enumerate which consumers mutate a value you want to share,
  make the value reject mutation (a `frozen` flag that throws on the mutators) and run the
  suite — each crash is a mutating call site, with a stack. Far faster than auditing call
  sites by hand.
- **When freezing / an assertion flushes a *cluster* of failures, suspect ONE shared
  copier/construction bug before concluding the aliasing is pervasive.** PR #1798's ~13-case
  flush looked like the construction pipeline aliased cached substructure everywhere; it was
  a single `AnnotatedTypeCopier` bug (it aliased the vararg type instead of copying it, so
  `deepCopy` was not actually deep). Fixing that one bug made all the freezing green.
- **A benign latent aliasing bug may have no wrong-result symptom.** That copier bug never
  produced a wrong diagnostic on master (its only post-copy mutator, defaulting, is
  idempotent), so no checker-test program demonstrates it without the freeze enforcement; the
  regression test (`checker/tests/nullness/VarargCacheAliasing.java`) only fails under
  freezing. A pure correctness fix like this needs a unit test asserting `deepCopy`
  independence, or it ships together with the enforcement that makes it observable.

## Shipping a risky correctness-sensitive hot-path change (the self-correcting safety net)

When the optimization is an algorithmic shortcut on a path where a *wrong* result is a
mis-diagnosis (not just slower) — e.g. the typeinference8 incorporation worklist (PR #1829),
which re-scans only the inference variables a newly instantiated one can affect, via
over-approximate reverse-dependency edges — the failure mode is "the shortcut skipped work it
shouldn't have." A self-correcting safety net makes that failure mode impossible to ship while
keeping the fast path:

- **Make the cheap path *confirm* itself against the full computation, and self-heal on
  mismatch.** The worklist declares a fixed point, then a full un-gated rescan verifies it; if
  the rescan finds more work, it applies it (marks everything dirty, re-propagates) so the
  result is **identical to the full scan by construction**. In production a worklist bug
  becomes a silent (still-correct, slightly slower) recovery, never a wrong diagnostic.
- **Turn the same check into a loud failure under a test-only flag.** A
  `-Dcf.typeinference.worklist.strict` system property makes the mismatch *throw* instead of
  self-healing; `build.gradle` sets it for the test tasks (the in-process `TypecheckExecutor`
  runs the checker in the test JVM, so a system property reaches it). CI then catches any
  edge-coverage regression as a hard failure, while users get the safety net. Net: correctness
  does not depend on the edges being complete — only performance does.
- **A regression-/stress test for this kind of change targets the *assumption*, not the
  feature.** `checker/tests/nullness/Java8InferenceWorklistStress.java` concentrates the
  inference shapes where a reverse-dependency edge is most likely missed (F-bounds, capture,
  intersection, nested generics, lub, lambdas, varargs, **and their combinations** — a missed
  edge most often hides where two mechanisms interact). It is `@SuppressWarnings`-ed because it
  exercises *inference*, not the type system; the strict-mode check is independent of
  diagnostics (it throws from inside inference).

### A/B for an always-on safety net: toggle it in ONE build, don't compare against master

If the change ships an always-on verification/self-heal step, the question reviewers ask is
"what does the *safety net* cost," not "what does the whole feature cost." Comparing
worklist-branch vs. master conflates two opposite-signed effects — the worklist *saves* scan
work, the always-on rescan *adds* it — and on realistic code they roughly cancel, so the diff
reads as noise and tells you nothing about either piece. Instead isolate the net within the
**same build**: a `-Pnoselfheal`-style gradle property that flips a `-D` flag on the forked
checker JVM, so self-heal-on vs self-heal-off run identical bytecode. (Confirm the toggle
actually reached the fork — print a one-line marker like `CF-SELFHEAL-DISABLED` at startup —
before trusting a flat result; a flag that never reaches the worker reads as "no effect" for
the wrong reason. See "a flat A/B can mean the workload never reached your code.") This is the
same one-variable-per-A/B and same-build-toggle discipline as the addressable-fraction habit,
applied to a safety net rather than a cache.

### Sizing a work budget / cutoff from real workloads

To set a cutoff that abandons pathological work (here `Java8InferenceContext`'s
`MAX_INCORPORATION_WORK`, lowered 100000 → 10000 in PR #1829, overridable via
`-AinferenceWorkBudget`), don't guess — **instrument the operation counter and take the max
over the whole suite plus a large real project.** Max real-code incorporation work was
framework 363, the stress test 296, Guava 994; a 10000 budget clears the worst real case ~10×
while still catching a cubic-in-nesting-depth blowup. Two gotchas: the budget is **checker-
independent** (Java-type incorporation dominates, so Nullness and Interning hit the same per-
problem cost — inference difficulty tracks the *types*, not the qualifier lattice); and the
context is created **per generic invocation**, not per compilation, so cache any per-factory
config (`getInferenceWorkBudget()`) rather than recomputing it. Validate a lowered budget by
re-running a large project (Guava) under several checkers and confirming **zero** new
budget-exceeded errors — a false positive there is a user-visible regression.

## What to look for

In order of historical hit rate on this codebase:

1. **Allocation hotspots** — TLAB allocation samples by class. Common
   culprits: `IdentityHashMap` resize storms, transient `ArrayList`/
   `HashSet` in visitor body, `Long`/`Integer` autoboxing.
2. **String/Name UTF-8 decoding** — `Name.toString()`, `Name.contentEquals`,
   `String.equals(Name)`. Cache the decoded form, or compare interned
   `String`s.
3. **Map double-lookup** — `containsKey` followed by `get`. Use
   `computeIfAbsent` or a single `get` + null check.
4. **`equals`/`hashCode` on annotated types** — these compound through
   visitor patterns. Caching pays back fast.
5. **Cold paths in hot wrappers** — a defensive copy or stream allocation
   inside a per-tree visitor.

## What to *not* propose without strong evidence

- Changes to public `checker-qual` API.
- Changes to `AnnotatedTypeMirror` equality/hash contract semantics —
  these affect interning and the SubtypeVisitHistory.
- "Refactor for readability" disguised as perf.
- Switching map implementations purely on micro-benchmark intuition —
  measure on a real workload.
- Lock-removal changes without auditing all reachable threads.

## Producing the patch

See [`.claude/skills/cf-patch-style/SKILL.md`](../cf-patch-style/SKILL.md)
for commit-message conventions. The performance-specific addition: the
commit body should cite the JFR self-time percentage and workload, e.g.
"JFR alltests trace attributed 8.4% of main-thread self-time to this
method".

## Verifying the patch

Before submitting, always:

1. `./gradlew assemble` — must succeed.
2. `./gradlew :framework:test :javacutil:test :dataflow:test` — fast.
3. The relevant checker test, e.g. `./gradlew :checker:NullnessTest`.
4. Ideally `./gradlew alltests` — the canonical regression catch. If it fails *only*
   on `:checker:jtregTests` / `:checker:jtregJdk11Tests` with `No java executable at
   java`, that is an environment issue, **not a regression**: `JAVA_HOME` is unset. Set
   it (`JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))`) and re-run those
   two tasks; the JUnit suites are unaffected.
5. Re-capture JFR on the **full** workload (`./gradlew --no-daemon
   checknullness`, all subprojects — confirm `phase` shows no scope warning)
   and confirm the targeted metric moved. Report the **whole-worker sample
   delta and wall-clock A/B** (caches vs. reverted, `shadowJar` rebuilt each
   side), not a single phase's inclusive %. A patch that passes tests but
   doesn't move the full-build profile is wrong by definition — and one that
   only moves a single-subproject slice is overstated.
