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
```

Self-time is computed from `jdk.ExecutionSample` ONLY. Including
`jdk.NativeMethodSample` pollutes the leaderboard with idle native frames
(`EPoll.wait` on the Gradle worker's messaging thread can be >50% of the
combined samples). `getThread()` is `null` in these traces, so per-thread
filtering does not work — but the compilation is single-threaded, so every
`ExecutionSample` is real work and no filtering is needed.

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
4. Ideally `./gradlew alltests` — the canonical regression catch.
5. Re-capture JFR on the **full** workload (`./gradlew --no-daemon
   checknullness`, all subprojects — confirm `phase` shows no scope warning)
   and confirm the targeted metric moved. Report the **whole-worker sample
   delta and wall-clock A/B** (caches vs. reverted, `shadowJar` rebuilt each
   side), not a single phase's inclusive %. A patch that passes tests but
   doesn't move the full-build profile is wrong by definition — and one that
   only moves a single-subproject slice is overstated.
