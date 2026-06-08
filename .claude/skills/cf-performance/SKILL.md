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

## Capturing a clean JFR trace

A naïve `./gradlew test` JFR capture **will be corrupted**: every JVM the
build spawns (the Gradle launcher, the daemon, the test worker, any forked
javac) inherits `JAVA_TOOL_OPTIONS` and, if they share one `filename=`,
they clobber each other and shred the constant pool — the trace comes back
with `<null>` thread names and is dominated by the launcher's idle frames
(`sun.nio.ch.EPoll.wait`, `ProcessHandleImpl.waitForProcessExit0`) instead
of the type-checking work. The capture script handles this by giving each
JVM its own per-PID file (the JFR `%p` filename token); use it.

```
./checker/bin-devel/record-jfr.sh -o cf.jfr -- \
    ./gradlew --no-daemon :checker:NullnessTest --tests "..." \
              -PmaxParallelForks=1
```

This writes `cf-<pid>.jfr` for each JVM. **The type-checking worker is the
largest file.** Pass all of them to the analyzer (below); the small
launcher/daemon files contribute almost no `ExecutionSample`s. With
`-PmaxParallelForks=1` and a `Test` task, Gradle's `forkEvery` may still
restart the worker, so the work can be split across two or three large
files — aggregate them.

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
5. Re-capture JFR on the same workload and confirm the targeted self-time
   percentage dropped. If it didn't, the patch is wrong even if the tests
   pass.
