# framework-perf

JMH micro-benchmark suite for hot paths in the EISOP Checker Framework.

## What this is for

JFR profiling on a real workload (a checker test suite, a downstream
project, or both — see `checker/bin-devel/record-jfr.sh`) tells you
which methods are hot. This module gives you a fast inner loop for
iterating on those methods: edit, recompile, get a number in seconds,
compare to baseline.

It is **not** a replacement for whole-compilation profiling. Numbers
from JMH only tell you the cost of the method in isolation; whether the
caller's call rate moved is a separate question that only a JFR run on
a representative workload can answer.

## Running

```bash
./gradlew :framework-perf:jmh
```

Forwarding arguments to JMH:

```bash
./gradlew :framework-perf:jmh -PjmhInclude=AnnotationMirrorSet
./gradlew :framework-perf:jmh --args="-prof gc"
./gradlew :framework-perf:jmh --args="-f 2 -wi 5 -i 5 -tu ns"
```

A typical run completes in under a minute per benchmark class with the
defaults in `build.gradle` (1 fork, 3 warmup × 5 measurement iterations).
For numbers you intend to cite in a commit message, raise `-f` to at
least 2 and `-i` to at least 5.

## What's covered today

- `AnnotationMirrorSetBenchmark` — `add`, `addAll`, iterate, `hashCode`
  at typical small-set sizes (1–3 elements).
- `AnnotationUtilsBenchmark` — `annotationName`, `annotationNameAsName`,
  `areSame` on marker annotations.
- `JavaExpressionBenchmark` — `LocalVariable` hashCode cached vs cold,
  reference-equality short-circuit on `equals`.

The benchmark set is intentionally narrow. The April–May 2026 campaign
targeted these methods with measurable success; this scaffold lets
future work verify the wins still hold and gives a baseline for the
short-list items in `docs/developer/performance-notes.md`.

## Adding a benchmark

1. Confirm the method shows in a JFR trace. Add to
   `performance-notes.md` first if it's a new candidate.
2. Construct realistic inputs in a `@State(Scope.Thread)` class. Reuse
   `BaseJavacState` for the processing-environment-backed objects;
   trial-scoped setup is amortized away from the measured region.
3. One benchmark method per code path. If the hot vs cold split
   matters (cache hit vs miss, fast path vs slow path), measure both
   separately.
4. Use `@OutputTimeUnit(TimeUnit.NANOSECONDS)` for sub-microsecond
   methods; otherwise `MICROSECONDS`.
5. Return the result or pass to a `Blackhole` — never let the JIT
   dead-code-eliminate the work you're measuring.

## What's deliberately not here

- **Whole-compilation benchmarks.** Those belong in
  `bin-devel/test-*.sh` orchestration; JMH is the wrong tool.
- **Benchmarks against a constructed `AnnotatedTypeMirror`.** Building
  a realistic ATM requires a full `AnnotatedTypeFactory` with a
  qualifier hierarchy, which is more setup than the current scaffold
  carries. If you need ATM-level benchmarks, the right move is to
  extend `BaseJavacState` with a tiny in-memory checker setup; the
  pattern exists in `framework-test`.
- **Benchmarks of public-API methods purely for the API surface.**
  Cache the question: would a 10% improvement here actually move a
  user-visible compilation time?

## CI

This module is not run by `./gradlew alltests`. Benchmarks are
opt-in; CI's job is correctness, not performance regression detection.
A future correctness check for the perf invariants themselves lives
in `framework/src/test/java/.../PerfRegressionTest.java`.
