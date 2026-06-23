# Performance notes for the EISOP Checker Framework

This document is the performance-engineering log for the framework: what
has been applied, what has been tried and rejected, and what is on the
short list to investigate. Its primary purpose is to stop contributors —
human or AI — from re-proposing changes that have already been merged or
already considered and discarded.

The canonical log of *behavioral* changes is [`docs/CHANGELOG.md`](../CHANGELOG.md);
this file is the *performance-engineering* log. Both should be updated
when a perf change ships, but the depth of detail belongs here.

Maintainership: append at the end of the relevant subsection, newest
last. Cite the merging PR number. Keep entries to a paragraph; if more
is needed, link to the PR.

The April–May 2026 optimization campaign that produced most of the
content below is summarized in the
[`3.49.5-eisop1` release notes](../CHANGELOG.md): compilation of a
4000+-file project with complex qualifiers went from ~30 minutes to
under 7 minutes; `allNullnessTests` from ~3:00 to ~2:30; `checkNullness`
from ~5:15 to under 4:00.

---

## Applied optimizations

### `AnnotatedTypeMirror` hashCode and equality

These compound across every visitor pattern and dataflow map operation,
so small per-call wins paid back substantially.

- **PR #1638** — *Several smaller performance optimizations.* The
  opening salvo, touching `AnnotatedTypeFactory`,
  `HashcodeAtmVisitor`, `EqualityAtmComparer`,
  `DefaultAnnotatedTypeFormatter`, `AnnotatedTypeReplacer`,
  `AsSuperVisitor`, and `AnnotationMirrorSet`. Includes the
  `AnnotationMirrorSet`-side groundwork for later PRs.
- **PR #1641** — *Further optimizations.* `AnnotatedTypeMirror`,
  `HashcodeAtmVisitor`, `AnnotatedTypeScanner`, more
  `AnnotationMirrorSet`.
- **PR #1644** — *Test reference equality before structural equality.*
  Added `if (this == other) return true` short-circuits to ~25
  `equals` methods across dataflow, framework, and javacutil
  (a sweep across 92 files). Co-authored with the Copilot SWE agent.
- **PR #1663** — *`AnnotatedTypeScanner#reduce` ordering and
  `HashcodeAtmVisitor` reduce improvement.* Reordered the reduce
  combiner so the cheap branch runs first.
- **PR #1667** — *Further optimize `ATM.hashCode` by simpler handling
  of primitive types.* Primitive-type ATMs are interned by `TypeKind`
  and have no qualifiers worth visiting; short-circuit directly to a
  fixed hash.
- **PR #1672** — *Avoid `Integer` boxing and lambda for ATM hashCode.*
  Rewrote `HashcodeAtmVisitor` from `SimpleAnnotatedTypeScanner<Integer, Void>`
  with a polynomial-hash reduce lambda to
  `SimpleAnnotatedTypeScanner<Void, Void>` with a mutable `int hash`
  accumulator. Removes per-node `Integer.valueOf` allocation and
  reduce-dispatch overhead.
- **PR #1675** — *Small optimizations.* Touched `AnnotatedTypeMirror`,
  `DefaultQualifierForUseTypeAnnotator`, `AnnotatedTypeScanner`,
  `QualifierDefaults`, `AnnotationUtils`, `ElementUtils`,
  `TypeKindUtils`, and two `typeinference8` files.
- **PR #1763** — *Constant `getKind()` overrides in ATM subclasses.* Added
  `@Override getKind()` returning the fixed `TypeKind` to every fixed-kind
  subclass: `AnnotatedDeclaredType`, `AnnotatedArrayType`,
  `AnnotatedExecutableType`, `AnnotatedTypeVariable`, `AnnotatedNullType`,
  `AnnotatedWildcardType`, `AnnotatedIntersectionType`, `AnnotatedUnionType`.
  Eliminates the heap hop through `underlyingType.getKind()`; on declared types
  that call includes `Symbol#apiComplete`. Only `AnnotatedPrimitiveType` and
  `AnnotatedNoType` fall through to the base, where the underlying `getKind()`
  is cheap.

### `AnnotatedTypeMirror` immutability foundation

- **PR #1798** — *`freeze()` mechanism + `AnnotatedTypeCopier` vararg-aliasing fix + frozen cache
  masters.* The foundation of the value-semantics program (full narrative under "AnnotatedTypeMirror
  value-semantics program" in Short list). Adds a `frozen` bit to `AnnotatedTypeMirror`, a
  `checkMutable()` guard on the three primary-annotation sinks (with
  `AnnotationMirrorSet.makeUnmodifiable()` as a backstop), and a cycle-safe deep `freeze()` that freezes
  only already-initialized components (lazy getters freeze what they create later). Freezes the master
  stored at all eight `AnnotatedTypeFactory` caches, so a latent in-place mutation of a cached type now
  fails fast with `BugInCF` instead of silently corrupting a shared value. Freezing flushed — and the PR
  fixes — a real `AnnotatedTypeCopier.visitExecutable` bug: it aliased the original's vararg type into
  the copy instead of copying it, so `deepCopy()` of an executable type was not fully independent.
  Caches still `deepCopy()` on every hit, so this is **behavior-neutral and measured perf-neutral**
  (deterministic allocation ±0.1% incl. a vararg-heavy workload; `freeze()` below the on-CPU sampling
  threshold). Shipped for the enforced invariant and the bug fix, not a perf number.
- **PR #1798 (cont.) — `classAndMethodTreeCache` boundary flip (kept); `elementTypeCache` flip
  (REVERTED).** The cross-cutting enabler was making `StructuralEqualityComparer.arePrimaryAnnosEqual`
  non-mutating (the Value Checker's override used to normalize its operands in place, which both prevents
  comparing a shared immutable type and is a side-effecting equality). Both post-pipeline caches were then
  flipped to **return the shared frozen master instead of `deepCopy()`ing on every hit**, with the minority
  of mutating callers copy-on-frozen at the mutation site. Measured win was modest: deterministic
  `ThreadAllocationStatistics` (median of 3) **−0.75%** (Big300) / **−0.97%** (Big600) on generic-call code,
  within noise elsewhere — ~1%, not the −5.3% an earlier estimate suggested (never reproduced against this
  baseline; the copier was already cheap, see the post-mortem above).
  **The `elementTypeCache` flip was then reverted: a full Guava nullness build (`test-guava.sh`, not covered
  by `alltests`) crashed with `BugInCF` "Attempted to mutate a frozen AnnotatedTypeMirror with underlying
  type java.lang.Object".** Root cause: a consumer lifts a *sub-component* of the shared frozen master — an
  unbounded wildcard's implicit `Object` upper bound, derived from a JDK generic's cached type-parameter
  bound (`Function<?, K>` with `K extends Comparable`) — into a *fresh, non-frozen* result type;
  `addComputedTypeAnnotations` then mutates the frozen child. The nine copy-on-frozen guards all copy at the
  **root** (`if (type.isFrozen()) deepCopy()`), so a non-frozen root holding a frozen child slips through —
  a hazard the root-level guard cannot catch and that escaped both `alltests` and the nine fixes. This is
  **structural to returning a shared frozen value**: any path that reparents a child of the shared master is
  a latent crash, unenumerable short of running every downstream project. For a ~1% win that is not worth
  it; reverted (commit message references the Guava crash). Regression test:
  `checker/tests/nullness/ElementTypeCacheWildcardBound.java` (minimized from
  `com.google.common.collect.SortedLists.binarySearch`). **The `classAndMethodTreeCache` flip is kept** — it
  is much lower-traffic (class/method declaration trees) and survived the full Guava build and `alltests` —
  but it carries the same residual embedded-frozen-component risk in principle; **re-run downstream builds
  (Guava et al.), not just `alltests`, before extending the shared-frozen-return pattern to any further
  cache.** The lesson: a frozen-master *tripwire* that still returns `deepCopy()` is safe (the master is
  never handed out); *returning* the shared frozen value is what creates the reparenting hazard.
  **Can the flip be salvaged? Three options, with a cost ladder (the answer is "not without copy-on-write"):**
  (1) *Fix each reparenting site* (the nine copy-on-frozen guards, plus a tenth for this wildcard/type-var
  bound). Cheap per site, but the obvious construction sites (`BoundsInitializer`, the wildcard visitor)
  already build fresh, so the frozen child enters through a subtler path; the guarantee needed ("nothing
  ever reparents a child of a shared frozen master") is a convention, not enforced — Guava found what
  `alltests` + nine fixes missed, and the next codebase could find an eleventh. Not shippable for ~1%.
  (2) *Deep guard at the choke point* — `deepCopy()` if **any** node is frozen, not just the root. Complete
  for the choke-point mutator, but the scan cost scales with the type and frozen children appear whenever a
  type embeds a cached generic bound (common), so it copies about as often as today and likely erases the
  ~1% — net-neutral-to-negative; measurement-gated, unpromising. (3) *Copy-on-write ATMs* — a frozen node's
  mutators return a fresh shallow node instead of throwing, so sharing is safe regardless of who reparents
  what; the whole bug class disappears. **This is the only complete fix, and it would make all eight caches
  flippable, not just `elementType` — so its payoff is the combined copy elimination, not ~1%.** It is a
  separate, measured architectural project (see "the recommended next direction" in the Short list and the
  copy-on-write notes below), not a patch to this PR. Verdict: keep the flip reverted; pursue the allocation
  win, if at all, via copy-on-write as its own effort.
- **Post-mortem: why the immutability allocation win came in at ~1%, not the projected large payoff.** The
  program was motivated by an earlier profile attributing `AnnotatedTypeCopier.visit` ~2% on-CPU self-time
  and **the dominant share of `Object[]` TLAB allocation (~22%)**. A fresh full-`checknullness` trace taken
  *after* this PR (11.8k `ExecutionSample`s, 156 s span) shows that figure is **stale**: `AnnotatedTypeCopier`
  is now **~0.76% self-time and ~1-1.5% of allocation**. The intervening work — the PR #1777
  `methodAsMemberOf`/`directSupertypes`/`elementType` caches, the thread-local copier `originalToCopy` map,
  and lazy `AnnotatedTypeScanner.visitedNodes` — had already harvested most of the copier allocation the
  immutability program was meant to remove. So by the time the boundary flips landed there was little copier
  cost left to delete, and the flip removes only the per-hit copy for the read-only-majority of consumers
  (≈1%). **Lesson: re-trace the current baseline before committing to an architectural plan built on an
  older profile — an allocation hotspot named in this log may already have been shrunk by later commits.**
  The current `checknullness` CPU profile is **flat** (hottest leaf `IdentityHashMap.get` at 2.98%, spread
  across ~10 callers); the largest remaining *addressable* allocation slices are `AnnotatedTypeScanner.markVisited`'s
  per-scan `IdentityHashMap` (~5% of allocation) and `AnnotationMirrorSet` construction+iteration (~6-10%),
  each a careful per-item job with low-single-digit wall-clock upside, not a large lever.

### `AnnotationMirrorSet` and annotation utilities

- **PR #1649** — *Reimplement `AnnotationMirrorSet` using an
  `ArrayList`.* Sets are small in practice; `TreeSet`'s `compareTo`
  (which decodes `Name` to `String` per comparison) was strictly more
  expensive than linear `areSame` on the observed sizes. Removed
  `NavigableSet` from the public interface — see CHANGELOG note. The
  patch initially shipped with a regression in `addAll` semantics; the
  fix preserves the non-standard fast-path return-`true`-if-any-new
  contract.
- **PR #1776** — *Index-based iteration of `AnnotationMirrorSet`
  on hot paths.* JFR `allNullnessTests -PmaxParallelForks=1` attributed
  80% of all `ArrayList$Itr` TLAB allocations (6,523 of 8,143 events,
  4.24% of total TLAB traffic) to `AnnotationMirrorSet.iterator()`, with
  the iterator-allocating callers concentrated in
  `AnnotatedTypeMirror.addAnnotations` (33%), `AnnotationMirrorSet.addAll`
  (14%), `ElementQualifierHierarchy`/`NoElementQualifierHierarchy`
  .findAnnotationInSameHierarchy (17.5% combined), and
  `AnnotatedTypeFactory.getDeclAnnotation` (10%). Added a public
  `AnnotationMirrorSet.get(int)` accessor (the backing store is already an
  `ArrayList`, so iteration order is stable) and routed those sites through
  index-based loops, following the overload-resolution pattern of PR #1775:
  `addAnnotations`/`addMissingAnnotations`/`replaceAnnotations` gained
  `AnnotationMirrorSet`-typed overloads that the ~69 call sites passing
  `getAnnotationsField()`/`getAnnotations()` bind to automatically;
  `addAll` got an `instanceof AnnotationMirrorSet` fast path; the two
  qualifier-hierarchy methods got an `instanceof` fast path; and
  `getDeclAnnotation`'s two loops (over an already-`AnnotationMirrorSet`-typed
  local) became index loops. Re-measured on the same workload: `ArrayList$Itr`
  dropped to 3,172 events (1.81%), `AnnotationMirrorSet.iterator()` calls
  dropped 6,523 → 1,530 (−77%), and `AnnotationMirrorSet$ReadOnlyIter`
  (751 events) left the profile entirely. The `Object[]`/`IdentityHashMap`
  allocation path and CPU self-time were unchanged. `CFAbstractValue.validateSet`
  was deliberately left alone: it runs only under `assert`, so its iterator
  allocation never occurs in production (`-da`) runs.
- **PR #1790 — index-based `AnnotationMirrorSet` iterator (June 2026).** PR #1776 converted the
  heaviest *direct* callers to `get(int)` loops, but the remaining `for`-each / `forEach` callers
  (`AnnotationUtils.areSame`/`getSame`, dependent-types and inference lambdas, ...) still went through
  `iterator()`, which was the single largest surviving `ArrayList$Itr` source (57% of it on the
  all-systems corpus). `iterator()` allocated the backing `ArrayList`'s own iterator *and*, for an
  unmodifiable set, wrapped it in a `ReadOnlyIter` — two allocations per traversal. `ReadOnlyIter` now
  walks the backing list by index (`get(i)`/`size()`) instead of wrapping an iterator, so the
  unmodifiable case (the common one) no longer allocates the `ArrayList$Itr` at all — the wrapper was
  already being allocated, so this is pure waste removed, and it fixes *every* caller (including
  lambdas) in one place with no caller churn. A **mutable** set still returns the backing iterator, on
  purpose: that preserves `remove()` and concurrent-modification detection, which a bare index walk
  cannot. Measured: `ArrayList$Itr` **5.39% → 3.68%** of TLAB events (the eliminated 58 samples are
  the unmodifiable iterations; the residual 38%-of-`iterator()` is mutable sets, kept for safety);
  deterministic all-systems allocation **5951 → 5884 MB (−1.1%)**, no normal-code regression; passes
  `alltests`. The other remaining iterator allocations are not cheaply/safely index-convertible:
  javac's cons-`List` (`get(i)` is O(i)), map iterators (`LinkedHashMap`), and `CollectionsPlume.mapList`
  over an unknown `List` impl.
- **PR #1669** — *Improve equality and comparisons of annotation
  names.* Introduced `AnnotationUtils.annotationNameAsName`, which
  returns the underlying `Name` without ever allocating a `String`. Hot
  callers that only need identity comparison or hashing now go through
  it. `Name` instances from the same `Elements` are guaranteed
  comparable by `==` within one javac invocation.

### `AnnotatedTypeScanner` iterator allocation

- **PR #1775** — *`scan`/`scanAndReduce` List overloads and `AnnotatedTypeCopier`
  index-based iteration.* Added `protected R scan(List<? extends ATM>, P)` and
  `protected R scanAndReduce(List<? extends ATM>, P, R)` overloads to
  `AnnotatedTypeScanner`. Java overload resolution prefers these over the
  `Iterable` versions, so all existing call sites in `visitDeclared`,
  `visitExecutable`, `visitIntersection`, and `visitUnion` automatically use
  index-based `list.get(i)` instead of an enhanced-for loop. Also changed
  `AnnotatedTypeCopier.visitDeclared` to iterate the raw `typeArgs` field
  (package-private, same package) by index instead of calling
  `getTypeArguments()` (which wraps in an unmodifiable list) and iterating
  with for-each. Combined JFR impact on `allNullnessTests -PmaxParallelForks=1`:
  `Collections$UnmodifiableCollection$1` TLAB events dropped from 3,113 (1.8%)
  to zero; `ArrayList$Itr` TLAB events dropped from 11,471 (6.7%) to 5,332
  (3.2%); total TLAB event count dropped 3.1% (171,829 → 166,464). Also added
  an `isEmpty()` short-circuit to `AnnotatedTypeMirror.getAnnotations()` that
  returns the shared `emptySet()` sentinel when `primaryAnnotations` is empty,
  avoiding a fresh `AnnotationMirrorSet` allocation for unannotated types.

### `AnnotatedTypeScanner` and visitor state

- **PR #1646** — *Only reset the visitedNodes if they are not empty.*
  Cheap guard before `reset()` — the common case is an already-empty
  map after the previous walk completed.
- **PR #1671** — *Increase the `AnnotatedTypeScanner#visitedNodes`
  map size.* Pre-sizes the `IdentityHashMap` to 64 to eliminate the
  early-resize storms previously visible in allocation profiles.
- **Re-measured June 2026** — `reset()` uses `new IdentityHashMap<>(VISITED_NODES_INITIAL_CAPACITY)`
  rather than `clear()` (the constant was then named `VISITED_NODES_EXPECTED_MAX_SIZE` and equal to 64). Leaf-frame self-time on `allNullnessTests -PmaxParallelForks=1`:
  `IdentityHashMap.clear` = 3.42% (668/19479 samples); `IdentityHashMap.init` net after
  background subtraction ≈ 1.27% (456 total − 180 background = 276 samples, /20809).
  `clear()` wins on object allocation (1.09% vs 1.48% of TLAB events) but loses on CPU:
  `IdentityHashMap.clear()` walks all 128 table slots explicitly in Java; TLAB allocation
  uses JVM bulk zeroing. The pre-sizing in PR #1671 is what makes `clear()` more expensive —
  pre-sizing enlarged the array that must be explicitly zeroed.
- **PR #1785 — reduced the pre-size from 64 to 8 (June 2026); renamed the constant to
  `VISITED_NODES_INITIAL_CAPACITY`.** Resolves the open candidate that used to sit in the
  short list below. The realistic worker (`checknullness`, all subprojects) showed `Object[]`
  at ~61% of TLAB allocations, ~91% of them these `IdentityHashMap` backing arrays from
  `AnnotatedTypeScanner.reset`/`<init>` and `AnnotatedTypeCopier.visit`; PR #1671's pre-size of
  64 backs a 256-slot `Object[]` per scan while most scans visit only 1–3 nodes. The constructor
  argument is `IdentityHashMap`'s `expectedMaxSize`, not a table size: it allocates a power-of-two
  backing array large enough to hold that many entries without resizing — so 4/8/16/32 back
  16/32/64/128-slot `Object[]`s that first resize at 6/11/22/42 entries, and 16 is byte-for-byte
  the no-arg default. A 4/8/16/32 JFR sweep (one full-build capture each) measured (on-CPU samples
  / wall span / GC collections / `Object[]` near CF / `reset`-site `Object[]`):
    - 4: 13,246 / 174 s / 667 / 21,490 / 2,803 — *worst*: resize-storm rehash on deeper types.
    - 8: 12,218 / 159 s / 458 / 36,767 / 8,379 — *best* GC and CPU; ~26% less map allocation than the default.
    - 16: 12,448 / 162 s / 504 / 43,091 / 11,370 — the JDK default size.
    - 32: 12,366 / 163 s / 554 / 86,226 / 31,019 — double the default allocation, no CPU gain.

  8 is the chosen value: resizing at 11 instead of 6 clears the 6–10-node tail that made 4 resize,
  so it matches the default on CPU/GC while still allocating less. Among 8/16/32 the CPU/wall
  numbers sit inside the ~3% run-to-run noise (a separate warm-daemon wall-clock A/B of 16 vs the
  shipped 64 — `shadowJar` rebuilt per side, first rep per block discarded, two interleaved passes
  — was a wash, both median 136 s over a 130–138 s spread), so the win is GC/footprint, not wall
  clock, exactly as the CPU-bound (~96% on-CPU, ~4% GC) profile predicts. An audit of the other
  ~60 `IdentityHashMap`s found just one more transient small-map worth pre-sizing —
  `ElementAnnotationUtil.annotateViaTypeAnnoPosition`'s `wildcardToAnnos` (≤2 entries, pre-sized to
  4 with its own rationale, unrelated to the visitor maps); the long-lived caches and per-analysis
  dataflow stores hold many entries and must keep the default, since pre-sizing them small would
  reintroduce resize storms.
- **PR #1763** — *Pre-sized `ArrayList` copies in `AnnotatedTypeCopier`.* Replaced
  `CollectionsPlume.mapList` lambda calls with direct pre-sized `new ArrayList<>(size)` loops.
  Removes lambda-dispatch overhead and allocates the destination list at the correct capacity
  immediately, avoiding internal growth copies.
- **PR # 1776** — *Reuse the `QualifierDefaults` defaulting scanner instead of
  constructing one per application.* `QualifierDefaults.applyDefaultsElement` created a fresh
  `DefaultApplierElement`, whose constructor created a fresh `DefaultApplierElementImpl` — an
  `AnnotatedTypeScanner` whose `visitedNodes` `IdentityHashMap` is pre-sized to 64 (a 256-slot
  `Object[]`) — for *every* type defaulted. On a realistic single compilation
  (`:checker:checkNullness`, isolated forked-javac worker, 3,337 samples) this was the largest
  single allocation source after the copier: `Object[]` was 61% of all TLAB events, and 17% of
  those `Object[]`s came from `AnnotatedTypeScanner.<init>`, of which 76% (3,360 events ≈ 8% of
  *all* TLAB events) were `DefaultApplierElementImpl` construction. The
  `AnnotatedTypeScanner` Javadoc explicitly says not to construct a scanner per use but to store
  and reuse one. Fix: `DefaultApplierElementImpl.outer` became non-final, and a single scanner is
  parked in a `QualifierDefaults.pooledApplierImpl` field and borrowed/returned around each
  `applyDefault` (`borrowApplierImpl`/`returnApplierImpl`). `AnnotatedTypeScanner.visit` already
  resets all scan state, so reuse is transparent. Safety: defaulting is not re-entrant into
  `applyDefault` (the scan only reads caches — `elementToBoundType`, `getPath` — and adds
  annotations; verified `getBoundType` and the per-location branches do not call back into
  `getAnnotatedType`/defaulting), and the pool is a size-1 slot that is `null` exactly while
  borrowed, so any *hypothetical* re-entrant borrow allocates a fresh scanner rather than
  corrupting the parked one — correctness never depends on non-re-entrancy. Confined to the javac
  main thread like the other caches. Re-measured on the same worker: `AnnotatedTypeScanner.<init>`
  `Object[]` allocations dropped 4,415 → 1,090 (−75%); `DefaultApplierElementImpl` (both the object
  and its map) left the allocation profile entirely; total TLAB events −5.7% (42,738 → 40,314) at
  an unchanged sample count; `DefaultApplierElementImpl.scan` self-time unchanged (no CPU
  regression). The 1,090 residual scanner constructions are now dominated (77%) by
  `ElementAnnotationApplier$TypeVarAnnotator`, addressed next.
- **PR #1776** — *Reuse two more per-use scanners: `TypeVarAnnotator` and the
  `isValidStructurally` structural scanner.* Same anti-pattern as the `QualifierDefaults` entry
  above, found by re-running the `:checker:checkNullness` allocation analysis after it.
  (1) `ElementAnnotationApplier.apply` constructed `new TypeVarAnnotator()` (a stateless
  `AnnotatedTypeScanner`) per call — 839 `Object[]` events, ~2% of TLAB, the largest remaining
  scanner construction. Pooled in a `static AtomicReference<TypeVarAnnotator>`: `getAndSet(null)`
  borrows, `set` returns. An `AtomicReference` (not a plain field, unlike the `QualifierDefaults`
  case) because `apply` is `static` and shared across factories/threads in the Gradle daemon and
  language server, and because `TypeVarAnnotator.visitTypeVariable` calls back into `applyInternal`
  (possible re-entrancy); a concurrent or re-entrant borrow sees `null` and allocates its own, so
  correctness never depends on single-threaded or non-re-entrant use.
  (2) `BaseTypeValidator.isValidStructurally` built a `SimpleAnnotatedTypeScanner` per call (234
  events). The validator is per-checker and main-thread-confined and the structural scan is not
  re-entrant (it is called once per top-level type from `isValid`, and its action only reads
  annotations), so the scanner is now a lazily-initialized field (lazy, not a field initializer, to
  avoid `this` escaping during construction; the captured `isTopLevelValidType` still dispatches to
  subclass overrides). Combined effect, measured across the four `checkNullness` worker traces:
  total `AnnotatedTypeScanner.<init>` `Object[]` allocations 4,415 → 1,090 (#1) → 290 (TypeVar) →
  48 (isValidStructurally), i.e. **−99% overall**. The 48 residuals are `TypesIntoElements$TCConvert`
  (30, ~0.08% of TLAB) and `typeinference8 InvocationType` (18) — both negligible; per-use scanner
  construction is no longer a meaningful allocation source. **Caveat (measured):** none of these
  moved single-compile wall-clock — `checkNullness` is not GC-bound at `-Xmx512m`, so the with/without
  delta was inside ±10% run-to-run noise (see the timing note). The value is GC/memory-pressure
  reduction (tight heaps, concurrent collectors, long-lived daemon/LSP JVMs), not single-compile
  latency. **General pattern** for any future scanner found constructed per use: a
  main-thread-confined scanner can reuse a plain size-1 pool field (like `QualifierDefaults`); a
  `static`/shared one needs an `AtomicReference.getAndSet` pool (like `TypeVarAnnotator`) to stay
  correct under daemon/LSP concurrency and re-entrancy — the `null`-while-borrowed state doubles as
  the re-entrancy guard.
- **PR #1794 — lazy `visitedNodes` (June 2026); encapsulation, measured perf-neutral.** Made
  `AnnotatedTypeScanner.visitedNodes` `private` and lazily allocated: the field starts `null` and
  the `IdentityHashMap` is created on the first stored node (`markVisited`) instead of in a field
  initializer, and `reset()` nulls the field instead of conditionally reallocating. The seven
  subclasses that touched the field directly (`BaseTypeValidator`, `TypesIntoElements`,
  `PropagationTypeAnnotator`, `QualifierDefaults`, `DependentTypesHelper`,
  `DoubleAnnotatedTypeScanner`, and the base class) now go through three `protected final` accessors
  `hasVisited`/`getVisited`/`markVisited`, centralizing the lazy-null invariant. **A/B (PR #1794
  vs. its merge-base):** deterministic `ThreadAllocationStatistics` on a 300-method file, 6
  interleaved rounds, was **−0.18%** total allocation (median 880.2 → 878.6 MB) — inside the
  ~0.5% run-to-run band; wall clock flat. A full-build `--no-daemon checknullness` JFR capture per
  side confirmed the *mechanism* — the eager `Object[]` allocation at `AnnotatedTypeScanner.reset`
  (the #1 CF `Object[]` site, ~29% of CF `Object[]` events on the base side) and at `<init>`
  disappears, reappearing only at `markVisited` (the lazy first-put) — but on-CPU samples, GC
  collections, and post-GC retained heap were all within single-run noise. **Why it is a wash:**
  PR #1646 already deferred the empty-case realloc, and the per-use scanner pooling above already
  removed ~99% of per-construction map allocations, so the base path was already mostly lazy; the
  lazy field only additionally skips allocation for the narrow "reset a non-empty map, then visit no
  recursive type" case. Distinct from the rejected `clear()` idea (see *Tried and rejected*): there
  is no explicit Java-loop zeroing — the win, where it exists, is *skipping* the allocation entirely,
  not making the reuse cheaper. Shipped for the encapsulation (one enforced place for the null
  invariant; `private` field; storage strategy decoupled from subclasses), not for a perf number.
  One source-compat note: `visitedNodes` going `protected` → `private` is incompatible for any
  third-party `AnnotatedTypeScanner` subclass that referenced the field directly.
- **PR #1815 — re-instantiate `IdentityHashMap`s instead of `clear()` (June 2026).**
  Applies the same principle as PR #1794 (fresh TLAB allocation is cheaper than an explicit
  `Arrays.fill` over the existing backing array) to four additional sites:
  (1) **`AnnotatedTypeCopier.visit`**: removes the PR #1791 thread-local map pool entirely.
  The pool's `finally { map.clear() }` was profiled at ~2.6% of `checknullness` self-time
  (see *Tried and rejected*: `AnnotatedTypeCopier.visit` pooled-map clear ratchet). The pool
  also required a re-entrancy fallback that allocated a new map anyway. Now `visit` always
  allocates a fresh `new IdentityHashMap<>(VISITED_NODES_INITIAL_CAPACITY)` and discards it
  on return — no pool, no clear, no re-entrancy guard.
  (2) **`AbstractQualifierPolymorphism.AnnotationMirrorMap.reset()`**: `visitedTypes.clear()` →
  re-instantiate `Collections.newSetFromMap(new IdentityHashMap<>())`.
  (3) **`EquivalentAtmComboScanner.Visited.clear()`**: `visits.clear()` → re-instantiate
  `new IdentityHashMap<>()`.
  (4) **`AtmLubVisitor.visit()`**: `visited.clear()` → re-instantiate.
  **Quick A/B** (cold-JVM wall clock, 3 reps/side, `gen-sized-program.py --shape generic`):
  master 300-method median ~5.9 s vs. branch ~6.2 s; 600-method ~8.3 s vs. ~8.2 s —
  **within cold-JVM noise (±0.7 s), no measurable wall-clock difference**. The win, if any,
  is GC pressure / allocation throughput rather than wall clock on heap-generous single-file
  runs; the `checknullness` JFR self-time attribution for `IdentityHashMap.clear` (2.6%)
  suggests the benefit would be clearest on a multi-CU warm-daemon workload.
- **PR #1827 — re-instantiate the per-CU / per-CFG `IdentityHashMap`s instead of
  `clear()` (June 2026).** Extends the PR #1815 principle to the long-lived, per-compilation-unit
  and per-CFG caches that PR #1791 had left on `clear()`:
  (1) **`AnnotatedTypeFactory.setRoot`** — the five tree caches (`classAndMethodTreeCache`,
  `fromExpressionTreeCache`, `fromMemberTreeCache`, `fromTypeTreeCache`, `elementToTreeCache`) plus
  the `scannedEnclosingTrees` identity set, now reassigned to fresh maps rather than cleared.
  (2) **`GenericAnnotatedTypeFactory`** — `scannedClasses`, `regularExitStores`,
  `exceptionalExitStores`, `returnStatementStores` (in `setRoot` and `performFlowAnalysis`).
  (3) **Dataflow `initFields`** — `AbstractAnalysis` (`inputs`, `nodeValues`, `finalLocalValues`),
  `ForwardAnalysisImpl` (`thenStores`, `elseStores`, `blockCount`,
  `storesAtReturnStatements`), `BackwardAnalysisImpl` (`outStores`, `exceptionStores`),
  `Worklist.depthFirstOrder` (re-assigned in `process`).
  (4) **`DefaultQualifierForUseTypeAnnotator.clearCache`** and **`TreePathCacher.clear`**.
  All affected fields were de-`final`ed with a doc note naming the sole reassigning method.
  **Why re-instantiate and not `clear()` for these:** the deciding factor is not per-CFG vs per-CU
  but whether the map's high-water mark can be inflated by a single large input. All of the above are
  *uncapped* — one giant method body (for the per-CFG maps) or one large compilation unit (for the
  per-CU caches) can blow them up. `clear()` retains that peak backing array for the rest of the
  build and re-zeroes it (`Arrays.fill`-style, O(capacity)) on every later, possibly tiny, reset —
  a potential super-linear cost (peak-capacity x number-of-later-resets) and a permanent memory
  high-water mark. Re-instantiation lets the big array be collected and starts each reset small; the
  per-reset allocation is negligible (and below the noise floor on the full-build A/B). The map
  content is new each reset anyway (keys are fresh per-CFG/per-CU identity objects), so `clear()`
  would preserve only capacity, never useful entries. `clear()` would only be the better choice for a
  genuinely *size-capped* reused map — none of these qualify.
  **Special case: `GenericAnnotatedTypeFactory.subcheckerSharedCFG`** stays on the null-guarded
  `clear()` (`clearSharedCFG`). It is *not* exempt for size reasons (it, too, grows with the CU);
  rather, unconditional re-instantiation broke two invariants: it must stay `null` for any factory
  that never shares CFGs (an ultimate parent with no subcheckers would otherwise allocate a
  forever-empty map every CU), and the lazy init in `addSharedCFGForTree` pre-sizes it to
  `getCacheSize()` (the field doc promises that initial capacity), which a default-capacity
  re-instantiation would defeat. A null-guarded `new IdentityHashMap<>(getCacheSize())` would also
  satisfy both and additionally shed the peak array, but since this map is reset only per-CU the
  retain-vs-shed difference is minor; the null-guarded `clear()` is kept for simplicity.
  **Correctness fix bundled in (the real motivation):** `AbstractAnalysis.setNodeValues` went from
  `nodeValues.clear(); nodeValues.putAll(in)` to `nodeValues = new IdentityHashMap<>(in)`. The old
  in-place `clear()` had an aliasing bug: `AnalysisResult` wraps the analysis's `nodeValues` in an
  `UnmodifiableIdentityHashMap` (read-through, no copy), and `AnalysisResult.getStoreBefore`/
  `getStoreAfter` pass that wrapper straight back into `runAnalysisFor` → `setNodeValues(in)`. The
  `nodeValues == in` guard does not fire (the argument is the *wrapper*, not the raw map), so
  `nodeValues.clear()` emptied the very map the wrapper reads, and `putAll(in)` then copied back
  nothing — wiping all node values. Copy-before-mutate (`new IdentityHashMap<>(in)`) reads the
  still-intact wrapper into a fresh map and leaves the original untouched. Observable in
  `dataflow/tests/constant-propagation/Expected.txt`: node abstract values (`a … > 0`,
  `b = a … > 0`, `4 … > 4`, `b … > T`) that the old code suppressed during visualization now
  appear; confirmed by running the test on both sides.
  **A/B** (full `./gradlew --no-daemon checknullness`, JFR, one run/side): on-CPU ExecutionSamples
  10530 (master) vs. 10655 (branch) — CPU-neutral, within run noise. `IdentityHashMap.clear` left
  the leaderboard entirely (0.50% / 53 samples → 0). The trade is more allocation churn: total TLAB
  events 81566 → 92713 (+13.7%, but `IdentityHashMap` per-class count flat at 2773 → 2798, so mostly
  sampling noise), GC 210 → 248 collections / 3258 → 4013 ms. Post-GC live heap measured *higher* on
  the branch (max 475 → 755 MB, median 243 → 315 MB), but on follow-up analysis this is almost
  certainly GC-timing noise across two separately-launched JVMs, not a real footprint regression:
  the genuinely retained per-CU dataflow memory is `GenericAnnotatedTypeFactory.flowResult` (the
  accumulator that `combine()` copies every CFG's `nodeValues` *entries* into — `flowResult` is
  reset per-CU in `setRoot` and is untouched by this patch). The theoretical divergence — after
  `getResult()` the `AnalysisResult` wrapper retains the per-CFG `nodeValues` (`MAP_A`) while a
  later `setNodeValues` reassigns the field to a copy (`MAP_B`) — is bounded to **one CFG** (the
  analysis's `getResultCache` and `nodeValues` are both reset in `initFields` before the next body)
  and to **one map**, i.e. kilobytes, not the hundreds of MB measured. A controlled warm-daemon
  `heap`-mode A/B/A/B would confirm, but no code change is warranted by it. Net: like PR #1815,
  wall/CPU-neutral and allocation-neutral (per-class `IdentityHashMap` flat); ship for the
  `setNodeValues` correctness fix and consistency with the established re-instantiate pattern, not
  for a throughput number. Follow-ons applied in the same PR:
  (a) the stale `setNodeValues` comment (which justified the `nodeValues == in` guard by the
  now-removed `clear()`-empties-`in` bug) was rewritten — correctness now rests on copy-before-mutate,
  and the `==`/`syncedFrom` guards are pure optimizations.
  (b) **Workaround removed:** `AnalysisResult.runAnalysisFor(node, preOrPost)` dropped its
  `copyNodeValuesIfNeeded()` defensive copy (and the comment/TODO explaining it). That copy existed
  only because the old in-place `setNodeValues` could mutate the map an `AnalysisResult` wraps; with
  copy-before-mutate the wrapped map is read but never mutated, so the copy is dead. On the hot
  store-query path it was already a no-op (`flowResult`'s `nodeValues` is private after the first
  `combine`). With that gone, the lazy copy-on-write also simplifies: the only remaining mutator is
  `combine()`, which copies all three wrapped maps together, so the separate `nodeValuesCopied` /
  `otherMapsCopied` flags and the `copyNodeValuesIfNeeded()` helper (which existed only to copy
  `nodeValues` independently for the spot-query path) collapse into a single `mapsCopied` flag and a
  single `copyMapsIfNeeded()`. The copy mechanism itself stays — the maps are read-only
  `UnmodifiableIdentityHashMap` views that `combine` must replace before mutating. Verified by
  `:framework:test`, `:checker:NullnessTest`/`ResourceLeakTest`/`MustCallTest`, and all dataflow
  tests.
  (c) **Regression test:** the constant-propagation dataflow test happened to cover this bug, but no
  Nullness test did — the bug only surfaces with verbose CFG visualization
  (`-Acfgviz=...StringCFGVisualizer,verbose`, which is the only caller of the unprotected
  `getStoreAfter(Block)`/`getStoreBefore(Block)`) and only on blocks rendered *after* the first
  node-bearing one (the wipe happens once, after the first block's contents are already emitted).
  `checker/tests/nullness-extra/cfgviz-nodevalues/` is a `nullness-extra` make test (a method with an
  if/else, so >1 node-bearing block) whose `Expected.out` carries the abstract values on the
  later-block nodes; it fails on the old `clear()`-based `setNodeValues` (values wiped) and passes on
  the fix.

### Element and name caching

- **PR #1645** — *Cache the methods in an element.* Adds a per-element
  method cache in `AnnotationFileParser` to avoid repeated
  `getEnclosedElements` filtering.
- **PR #1648** — *Optimize determining boxed primitive types.* Adds a
  `TypeKindUtils` fast path that avoids a `Types.boxedClass` call
  followed by `getQualifiedName` when the kind alone is sufficient.
- **PR #1673** — *Cache qualified, interned names for all elements.*
  Adds an `IdentityHashMap`-backed cache in `ElementUtils` keyed on the
  `QualifiedNameable`. Routes the six hottest `Name.toString()` call
  sites (`AnnotationUtils`, `AnnotationBuilder`, `TypesUtils`,
  stub parser, etc.) through it. Also removes the now-redundant
  `AnnotationUtils#annotationNameInterned` — `annotationName` itself
  now returns an interned name. See CHANGELOG note.
- **PR #1763** — *`ElementUtils.parentPackage` fast path.* When the `PackageElement` is a
  javac `Symbol.PackageSymbol`, reads the enclosing package directly from the `owner` field
  instead of calling `Elements#getPackageElement(String)`. Falls back to the original
  string-based lookup for non-javac implementations.
- **PR #1796** — *Interned-`Name` identity comparison for fixed name literals.* New
  `InternalUtils` helpers (`isInitName`, `isThisName`, `isSuperName`, `isValueName`,
  `isJavaLangObjectName`, `isJavaLangEnumName`) compare a javac `Name` against its own
  table's pre-interned name (`n == n.table.names.init` etc. — uses the *name's own* table, so
  no cross-context identity assumption) with a `contentEquals` fallback for non-javac
  `Name`s. Converted the utf2chars-profiled sites — `TreeUtils.isConstructor`/
  `isEnumSuperCall` (the latter also reordered to check `<init>` before the class name), the
  `this`/`super` identifier checks in `TreeUtils`,
  `TypeFromExpressionVisitor.visitIdentifier`/`visitMemberSelect`, `ParamApplier.isReceiver`,
  `ElementUtils.isObject` — and then the remaining ~25 fixed-literal `contentEquals` sites
  across dataflow (`CFGTranslationPhaseOne`, `JavaExpression`, `SuperNode`,
  `ExplicitThisNode`), framework, and the checkers (initialization, interning, nullness,
  lock, units). Full-build `checknullness` JFR: every converted site left the
  `Convert.utf2chars`/`utf2string` attribution (was ~30 of 275 utf2* samples ≈ 0.23% of all
  samples), so the end-to-end effect is real but sub-0.5% — not resolvable in wall clock.
  Microbenchmark on a byte-backed name table: 12x faster and ~66 B/op allocation removed vs
  `contentEquals`; neutral on `StringNameTable`. Only names with pre-interned `Names` fields
  available on JDK 11+ are used (`init`, `_this`, `_super`, `value`, `java_lang_Object`,
  `java_lang_Enum`); `names.yield` (JDK 13+) was deliberately not used.

  **General `sameName(Name, CharSequence)` with a table-validated static cache (also
  PR #1796).** For arbitrary (non-pre-interned) target strings, the naive per-call form
  `n == n.table.fromString(literal)` is a dud — measured (8.2M mixed hit/miss ops): ~12%
  faster but 28% *more* allocation than `contentEquals` on a byte-backed table (it
  re-encodes the literal per call), and 1.8x *slower* on `StringNameTable`. A naive static
  `Map<String, Name>` is unsound in multi-compilation JVMs (the test suite, a language
  server): a cached `Name` from a previous compilation's table compares `==`-false against
  content-equal names from the new table. The applied design closes both holes: a single
  `volatile` holder pinning `(Name.Table, ConcurrentHashMap<String, Name>)` that
  `sameName` discards whenever it sees a name from a different table — stale answers are
  impossible, the worst case is a cache rebuild on table switch. Measured: **5.8 ns/op,
  zero allocation vs `contentEquals`'s 36.8 ns/op + ~66 B/op on byte-backed tables (6.4x)**;
  on `StringNameTable` 5.7 vs 4.7 ns/op (~neutral). Converted the dynamic-but-bounded-target
  sites: `AnnotationUtils.getElementValue`'s element-name loops (the **#1 utf2* consumer for
  the Resource Leak Checker** — 66 of 201 utf2* samples on `checkResourceLeak`, invisible on
  `checknullness`; hot-site profiles are checker-specific), `AnnotationBuilder.findElement`,
  `ElementUtils`/`TreeUtils` method/field-name lookups, `JavaExpressionParseUtil` identifier
  resolution, the stub parser's `findElement` family, and `SetOfTypes.anyOfTheseNames` (via
  `ElementUtils.getQualifiedName`'s interned cache). Cardinality caveat: each distinct probe
  string is interned into the compiler's name table and cached for the compilation, so
  `sameName` is only for bounded target sets (annotation element names, configured method
  names, source identifiers) — not arbitrary unbounded input.

  **Key environmental facts (verified June 2026):** (1) which `Name.Table` javac uses decides
  whether `Name.toString()`/`contentEquals` decode UTF-8 per call: byte-backed
  `SharedNameTable` is the default before JDK 23, `StringNameTable` (decode-free, cached
  `toString`) since JDK 23. (2) **Gradle passes `-XDuseUnsharedTable` to every forked javac**
  (verified in a `--debug` compile log), forcing the byte-backed table on *all* JDK versions —
  so under Gradle (this project's own build, most users' builds) the decode cost is alive on
  JDK 25/26 too, while plain-javac/Maven runs on JDK 23+ don't have it. Measure name-decode
  changes with `-XDuseUnsharedTable`, or the A/B silently tests the wrong table. (3) Do NOT
  compare a `Name` char-by-char (`charAt` loop): base `Name.length()`/`charAt()` call
  `toString()` per invocation, so that is N+1 decodes instead of `contentEquals`'s one
  (measured 2.8x slower interpreted, 545 MB extra allocation per 8M ops on JDK 21); the
  raw-byte APIs (`getUtf8Length`/`getUtf8Bytes`/`map`) are version-specific and *re-encode*
  on `StringNameTable` (measured 5x slower) — identity against an interned `Name` is the only
  variant that wins on every table.

### Annotation-file (stub) parsing

- **PR #1776** — *Share the annotated-JDK stub AST across compilations.*
  Inclusive-time analysis of `allNullnessTests -PmaxParallelForks=1` (the run is
  many small per-directory compilations in one worker JVM) showed
  `AnnotationFileParser.parseStubFile` at ~32% and the JavaParser parse itself
  (`com.github.javaparser.*`) at 14.4% of execution samples — the annotated JDK is
  re-read and re-parsed from scratch by every compilation, because `stubTypes` is a
  per-`AnnotatedTypeFactory` field. The JDK stub text is fixed for a given JVM and
  its JavaParser AST does not depend on the javac context (only the later
  `process*` resolution does), and JDK-stub processing is read-only on the AST
  (verified: the only AST mutation, `concatenateAddedStringLiterals`, is
  ajava-only). So `AnnotationFileParser.parseStubUnit` now memoizes the
  `StubUnit` for `JDK_STUB` files in a static `ConcurrentHashMap` keyed by jar-entry
  name; each compilation still re-runs `process*` against its own model. Re-measured
  on the same workload: `parseStubUnitForJdk` inclusive dropped 10.0% → 2.1%,
  `com.github.javaparser.*` 14.4% → 7.2%, the JavaParser allocation classes
  (`Token` 1,643, `Position` 1,612, `JavaToken` 1,023, `Range` 784 TLAB events)
  left the top-35 entirely, and total TLAB events fell 3.4% (175,677 → 169,675).
  A single user compilation parses each JDK class once either way, so the win is
  for multi-compilation JVMs: the test suite (a tracked metric), the Gradle daemon,
  and the language server. The cache is bounded by the number of distinct JDK stub
  classes (a few hundred) and is shared, so it is a fixed cost, not per-compilation
  garbage. Correctness re-verified with `allNullnessTests`, `IndexTest`,
  `SignatureTest`, `NullnessTest`, `InterningTest`, `ValueTest`, and the
  `:checker:test`, `:framework:test`, `:javacutil:test`, `:dataflow:test` suites.
- **PR #1797 — `IdentityHashMap<Name, TypeElement>` for annotation name maps (June 2026).**
  The annotation-name lookup maps in `AnnotationFileParser`, `InsertAjavaAnnotations`, and
  `TypeAnnotationMover` previously used `HashMap<String, TypeElement>`, requiring `Name.toString()`
  (a UTF-8 decode on byte-backed tables) at every map-build site. Changed to
  `IdentityHashMap<Name, TypeElement>`: keys are the `Name` objects returned by
  `getSimpleName()` / `getQualifiedName()` directly, and lookups use `elements.getName(s)` to
  intern a JavaParser `String` into the same table, guaranteeing identity equality within one
  compilation. Also removed a redundant `elements.getName(annoElt.getSimpleName())` call in
  `AnnotationFileParser.getImportedAnnotations` — `getSimpleName()` already returns an interned
  `Name` from the same table, so the round-trip was a decode-and-re-intern no-op. Safety: all maps
  are built and consumed within a single compilation's `Elements` instance, so same-table identity
  holds; the `getAnnotation` fallback (`elements.getTypeElement(fqn)` + `createNameToAnnotationMap`)
  handles first-encounter FQN annotations and populates both simple-name and FQN entries for future
  hits.

- **PR #1776** — *Avoid the defensive deep copy in read-only
  `fromElement` consumers.* `AnnotatedTypeFactory.fromElement` returns
  `cached.deepCopy()` on every cache hit so callers may mutate the result; this is
  the second-largest `Object[]` allocation source (`AnnotatedTypeCopier.visit`, the
  per-copy `IdentityHashMap`). Added `getElementAnnotations(Element)`, which returns
  the cached type's primary annotations directly (`getAnnotations()` already returns
  an unmodifiable set and cached types are never mutated, so this is safe), and
  routed `DefaultQualifierForUseTypeAnnotator.getExplicitAnnos` — a read-only caller
  that only needs the element's primary annotations — through it. Honest impact note:
  on the profiled workloads the measured delta is within noise, because
  `getExplicitAnnos` runs ~95% of the time *during* stub parsing, where the element
  cache is cold and `fromElement` takes the compute (no-copy) path anyway. The
  change is correct and removes the copy on the warm-cache path (repeated
  default-for-use queries on already-cached elements, as in large multi-round
  projects); it is kept on that basis, not on a measured win here.

### Cache sizes and synchronization

- **PR #1665** — *Increase default cache size from 300 to 2000.*
  Profiling showed the LRU eviction rate dominated some workloads;
  the win from cache-hit rate exceeds the memory cost at modern heap
  sizes.
- The `Collections.synchronizedMap` wrapper around
  `AnnotatedTypeFactory.annotationClassNames` is gone. The wrapper was
  carried over from a 2020 refactor of a previously static
  `AnnotationUtils` cache; per-factory instance fields don't need it,
  matching every other LRU cache on the same object. AT factories are
  confined to the javac main thread.

### Qualifier hierarchies

- **PR #1670** — *Add extra maps to qualifier hierarchies.* Added
  identity-keyed caches in `ElementQualifierHierarchy` and
  `NoElementQualifierHierarchy` to avoid repeated `annotationName`
  lookups in `findAnnotationInSameHierarchy` and adjacent hot paths.
  Made `elements` field protected so subclass hierarchies can extend
  the same caching pattern.
- **PR #1763** — *Empty-collection early-out in `ElementQualifierHierarchy`.* Added an
  `annos.isEmpty()` guard at the top of `findAnnotationInSameHierarchy` to return immediately
  without entering the qualifier-kind lookup loop.

### Dataflow expressions

- **PR #1643** — *Cache the hashCode for dataflow expressions.* Added
  a cached `hashCode` field across `ArrayAccess`, `ArrayCreation`,
  `BinaryOperation`, `FieldAccess`, `FormalParameter`, `LocalVariable`,
  `MethodCall`, `UnaryOperation`, and `ValueLiteral`. Per-object cost
  varies: `LocalVariable` pays zero because of the existing alignment
  gap; `FieldAccess` and similar pay +8 bytes. Peak overhead measured
  at ~128 bytes for a large method, well worth the savings on store-
  comparison hot paths.
- **PR #1797 — `LocalVariableNode.hashCode`/`equals` avoid `getName()` and `Objects.hash()` (June 2026).**
  Both methods previously called `getName()`, which calls `Name.toString()` (a UTF-8 decode on
  byte-backed tables). Changed to read the `Name` directly from the tree
  (`IdentifierTree.getName()` / `VariableTree.getName()`) for both operations: `equals` uses
  `InternalUtils.sameName`; `hashCode` calls `name.hashCode()` directly — on `SharedNameTable`
  this returns the byte-table `index`, which is content-stable via interning, no decode needed.
  Also removes the `Objects.hash(name)` varargs call, which allocated an `Object[]` per invocation
  (the varargs antipattern flagged in Applied optimizations → Generic map/lookup patterns).

- **PR #1765** — *`BinaryOperation.hashCode` symmetry fix.* For commutative operations,
  `equals()` ignores operand order; the hash code must match. Replaced the
  order-dependent `Objects.hash(kind, left, right)` with
  `Objects.hash(kind, left.hashCode() + right.hashCode())` so that `a OP b`
  and `b OP a` hash identically. This is a correctness fix for the `equals`/`hashCode`
  contract that also improves cache hit rates for commutative expressions.
- **PR #1812 — eliminate `Objects.hash` boxing and fix zero-hash caching.** Two
  related fixes applied across the remaining `JavaExpression`
  subclasses (`ArrayAccess`, `BinaryOperation`, `FormalParameter`, `LocalVariable`,
  `MethodCall`, `UnaryOperation`, `ClassName`) and several framework/javacutil classes
  (`CFAbstractValue`, `DiagMessage`, `AnnotationMirrorSet`):
  (1) **`Objects.hash` removal.** Each `hashCode()` that used `Objects.hash(...)` was
  rewritten to the equivalent `h = 31 * h + field.hashCode()` polynomial, eliminating
  the varargs `Object[]` allocation and autoboxing per call (see the "Gotcha" entry
  in Generic map/lookup patterns for the general rule).
  (2) **Zero-hash sentinel fix.** The lazy `hashCodeCache == 0` guard used by
  PR #1643 treats 0 as "not yet computed". A hash that genuinely computes to 0 would
  be recomputed on every call, defeating the cache. Fixed throughout with
  `hashCodeCache = h == 0 ? 1 : h`, remapping the all-zero case to 1.
  (3) **`QualifierVar` gains a cached hash code.** `QualifierVar.hashCode()` was
  previously uncached despite calling `Objects.hash(id, invocation, polyQualifier)`
  (where `invocation.hashCode()` can itself be expensive). Added a `cachedHashCode`
  field with the same lazy + zero-sentinel pattern.

### Dataflow stores, analysis, and transfer

- **PR #1664** — *Improve `hashCode` implementation for
  `CFAbstractStore`.* Cleaner accumulation; avoids redundant work on
  empty sub-stores.
- **PR #1686** — *Small optimizations/clarifications in
  dataflow/analysis.* Touched `AbstractAnalysis`, `AnalysisResult`,
  `BackwardAnalysisImpl`, `ForwardAnalysisImpl`.
- **PR #1688** — *Use identity for dataflow worklists.* `IdentityHashSet`
  semantics for the worklist — block identity is what matters, not
  block equality.
- **PR #1691** — *Small `BackwardAnalysisImpl` and
  `ForwardAnalysisImpl` improvements.* Authored by Copilot; reduces
  redundant work in the two analysis impls.
- **PR #1696** — *`CFAbstractTransfer` fixes and optimisations.*
  Includes the fix for the `IndexOutOfBoundsException` for lambdas in
  varargs with Aliasing-Checker subcheckers, and switches
  `CFAbstractTransfer` to return `RegularTransferResult` for non-
  boolean returns instead of always wrapping in
  `ConditionalTransferResult`. Downstream effect: checkers that need
  a `ConditionalTransferResult` for non-boolean methods must update
  their transfer functions. See `NonEmptyTransfer` for the pattern.
- **PR #1707** — *Review of dataflow package.* Touched
  `ControlFlowGraph`, `ConstantPropagationStore`, `JavaExpression`,
  `MethodCall`, `PurityUtils`, and the live-variable + reaching-def
  stores. Both a perf and clean-up pass.
- **PR #1817 — `CopyOnWriteMap` for `CFAbstractStore` maps + store-merge optimizations (June 2026).**
  Four independent changes to reduce store-copy overhead during forward analysis:

  (1) **`CopyOnWriteMap<K,V>` (new class).** A `Map<K,V>` wrapper that defers copying its delegate
  `HashMap` until the first mutation after a `copy()` call. Store copy operations — which occur at
  every block join — previously created five `new HashMap<>(other.map)` allocations; with `CopyOnWriteMap`
  they share the delegate reference and copy only on the first `put`/`remove`/`clear`. The `equals`
  method adds a delegate-identity fast path (`this.delegate == other.delegate`), so unmodified copies
  compare equal in O(1). `hashCode` is cached with the zero-sentinel pattern and invalidated on mutation.
  **Caution:** `keySet()`, `entrySet()`, and `values()` return the raw delegate's views; mutations through
  those views bypass `ensureUnshared()`. All in-package callers use `put`/`remove`/`clear`, but any future
  iterator-based removal must go through the map's own `remove`, not the view's `Iterator.remove()`.

  (2) **`leastUpperBound`/`widenedUpperBound` early-exit when equal.** `CFAbstractStore`,
  `InitializationStore`, `NullnessNoInitStore`, and `LockStore` each gain a `this.equals(other)` guard
  at the top of their LUB methods, returning `this.copy()` immediately when the stores are identical.
  With `CopyOnWriteMap`, this check hits the delegate-identity path (O(1)) for stores that have not
  diverged since their last shared copy — the common case in a fixpoint that has nearly converged.

  (3) **Smaller-map iteration in `upperBound`.** When the two stores differ, the merge iterates the
  smaller map and looks up in the larger, reducing the number of `get` calls by half when one store
  has a subset of the other's keys (common when one branch adds more refinements). A per-value
  `thisVal.equals(otherVal)` short-circuit in `upperBoundOfValues` avoids calling `leastUpperBound`
  or `widenUpperBound` on already-equal values.

  (4) **`ForwardAnalysisImpl` copy-once per block.** Previously each uncached node in a regular block
  called `callTransferFunction(n, store.copy())`, allocating a new `TransferInput` per node. The copy's
  sole purpose was to protect `blockTransferInput` (the block's cached entry state, stored in `inputs`)
  from in-place mutation by the transfer function. Since `store` is replaced by
  `new TransferInput<>(n, this, transferResult)` after the first node regardless, only the initial
  copy (before the first uncached node) is necessary — subsequent nodes receive a fresh `TransferInput`
  wrapping the previous node's result, not `blockTransferInput`. One copy per block replaces one copy
  per node.

  Also fixes `Objects.hash` / varargs boxing in `TransferInput`, `Constant`, `ArrayCreation`,
  `FieldAccess`, `ValueLiteral`, `AnnotatedTypeParameterBounds`, `Contract`, `Default`,
  `DependentTypesError`, and `Pair` (consistent with PR #1812's sweep); adds `equals`/`hashCode`
  to `NullnessNoInitValue`; and reduces jtreg timeout budgets for `Issue1438{,b,c}.java` (90/120/50 s
  → 20 s each), which are the regression tests for the quadratic-store fixpoint issue these changes
  fix.

  **Quick A/B (3-rep cold-JVM, `gen-sized-program.py --shape generic`, `assembleForJavac` rebuilt per
  side):**

  | methods/file | master alloc | branch alloc | Δ alloc | master wall | branch wall | Δ wall |
  | --- | --- | --- | --- | --- | --- | --- |
  | 300 | 850.70 MB | 835.60 MB | −1.8% | 6.13 s | 6.31 s | +0.18 s |
  | 600 | 1432.60 MB | 1399.70 MB | −2.3% | 8.32 s | 7.93 s | −0.39 s |

  Allocation is a consistent −2% across both sizes; wall clock differences are within the ±0.7 s
  cold-JVM noise floor. JFR self-time: `CFAbstractTransfer.addFinalLocalValues` (1.40% of samples on
  master) drops to absent on branch; total `ExecutionSamples` 571 → 493 on a single Big600 run (within
  single-run noise). `CopyOnWriteMap` does not appear as a CPU leaf — all hot paths go directly to the
  delegate `HashMap`. For the actual fixpoint-convergence speedup, measure on a real project with
  `./gradlew --no-daemon checknullness` (warm-daemon reps); the generic-shape tiny-file corpus
  stresses per-file overhead, not the fixpoint loop.

### Generic map/lookup patterns

- **PR #1692** — *Avoid contains/get for maps that contain no null
  values.* Replaces the `if (m.containsKey(k)) m.get(k)` antipattern
  with a single `get` plus null check at multiple call sites in
  `AnnotatedTypeFactory` and `GenericAnnotatedTypeFactory`. The
  precondition (no null values) is documented inline at each site.
- **PR #1693** — *Avoid duplicate checks.* Cross-references between
  type factories had redundant precondition checks; consolidated.
- **PR #1694** — *Guard log calls by debug flag.* `String.format`
  arguments were being evaluated for log messages that would be
  discarded. Wrapped in `if (debug)` guards where present.
- **PR #1695** — *Optimize `TreePathCacher` usage.* Avoid recomputing
  `TreePath` when the cache already has the answer.
- **PR #1763** — *`TreePathCacher` control-flow exception optimization.* The
  `Result` exception used for non-local exit is constructed with
  `super(null, null, false, false)` to suppress stack-trace generation; the
  exception is caught two frames up and never logged or rethrown.
- **PR #1765** — *`entrySet()` iteration over `keySet() + get()`.* Applied the
  pattern across `UBQualifier`, `LockAnnotatedTypeFactory`, `MustCallInference`,
  and `AnnotationConverter`: iterate `map.entrySet()` instead of `map.keySet()`
  followed by `map.get(key)`, eliminating a redundant second hash lookup per
  iteration.
- **PR #1781** — *`IdentityHashMap` for caches keyed by `Element`/`Tree`.* javac `Symbol`s
  and `JCTree`s use identity `equals`/`hashCode` (they do not override `Object`'s),
  so a `HashMap` keyed by them was *already* an identity map — switching to
  `IdentityHashMap` does not change behavior, it just drops the per-entry `Node`
  allocation (open addressing) and replaces the virtual `hashCode()`/`equals()`
  dispatch with `System.identityHashCode`/`==`. Converted six long-lived,
  identity-keyed maps that had been left as `HashMap`:
  - `AnnotatedTypeFactory.cacheDeclAnnos` (`Element` → `AnnotationMirrorSet`;
    populated by the hot `getDeclAnnotations`),
  - `GenericAnnotatedTypeFactory.subcheckerSharedCFG` (`Tree` → `ControlFlowGraph`;
    pre-sized to `getCacheSize()`),
  - `GenericAnnotatedTypeFactory.scannedClasses` (`ClassTree` → `ScanState`),
  - `TreePathCacher.foundPaths` (`Tree` → `TreePath`),
  - `CFGTranslationPhaseOne.parenMapping` (`Tree` → `ParenthesizedTree`; built
    per-method during CFG construction),
  - `AbstractAnalysis.finalLocalValues` (`VariableElement` → abstract value) —
    this one was the lone `HashMap` among siblings (`nodeValues`, `inputs`,
    `treeLookup`, `postfixLookup`) that were *already* `IdentityHashMap`.

  **Safety rule for this conversion.** `IdentityHashMap.equals`/`hashCode` compare
  *values* by reference too (they intentionally violate the `Map.equals` contract),
  so only convert a map that is never `Map.equals`-compared. The dataflow *fixpoint*
  compares `CFAbstractStore`s, not these maps, and none of the six is passed to
  `Map.equals` — verify this before converting any further map. Audited but **left
  as `HashMap`**: method-local short-lived maps (`InferenceResult`,
  `DependentTypesHelper.checkTypesForErrorKey`, `BaseTypeVisitor` javaparser pairing)
  where there is no entry-allocation pressure to remove, the stub-parser
  `AnnotationFileParser.atypes` (cold for real workloads — stub parsing only
  dominates in test-harness amplification), and the 4-entry
  `LombokSupport.defaultedElements`.

  **Measured impact (full-build A/B, June 2026).** This is an allocation/dispatch
  reduction, not a measurable speedup. Wall clock on `./gradlew checknullness` (all
  ~10 subprojects, warm daemon, processor `shadowJar` rebuilt each side, median of
  ≥3 warm reps) was **~2m34 s with vs. ~2m32 s without — within run-to-run noise**;
  the build-level gain is below the wall-clock floor. The mechanism is real but small
  in the JFR profile (full-build `--no-daemon` traces): `HashMap$Node` dropped from
  **3.21% → 2.76%** of TLAB allocation events (6,856 vs. 7,315 absolute — fewer even
  though that branch trace sampled ~10% *more* total allocation), and with the
  `[Ljava.util.HashMap$Node;` backing arrays the HashMap internals fell **4.66% →
  4.02%**. Leaf self-time in `HashMap.getNode` fell **3.38% → 2.27%**, partly offset by
  `IdentityHashMap.get` rising **1.28% → 1.50%** (cheaper per call: `identityHashCode`
  + `==` + flat-array probe vs. virtual `hashCode`/`equals` + `Node` chase). Retained
  memory was **unchanged**: post-GC live heap maxed at 512 MB on both sides with p90/median
  within noise, and GC count/summed-pause were flat (647/7.86 s vs. 660/7.93 s) — the flat
  `Object[]` of `IdentityHashMap` is roughly memory-neutral against `HashMap`'s
  `Node[]`-plus-`Node`s for these small, long-lived maps. The takeaway: file such
  identity-map conversions for their cumulative GC-pressure relief, not for a standalone
  wall-clock win.
- **Gotcha — avoid `Objects.hash(...)` / `Arrays.hashCode(...)` on hot paths.**
  `Objects.hash(a, b, ...)` is varargs, so each call allocates an `Object[]` *and*
  autoboxes every primitive argument to its wrapper — e.g. `Objects.hash(int, int)`
  is three allocations (the array + two `Integer`s) per call. On a per-node /
  per-invocation path (cache-key constructors, `hashCode()` overrides) write the
  polynomial by hand instead: `int h = 31 * a + b;` (or `h = 31 * h + next;` for
  more fields). This mirrors the `HashcodeAtmVisitor` boxing/lambda removal
  (PR #1672) and was applied to the `methodFromUse` cache-key constructor
  (`MethodAsMemberOfCacheKey`), which builds a key on every cached method
  invocation. The two-arg `Objects.equals` is fine (no array/boxing); only the
  varargs `hash`/`Arrays.hashCode` family allocates. Precompute the result into a
  `final int hash` field when the key is immutable, as both new cache keys do.
  A follow-up sweep (PR #1812) applied the same rewrite to the remaining
  `JavaExpression` subclasses and framework classes still using `Objects.hash` in
  their cached `hashCode()` implementations, and also fixed the zero-hash sentinel
  bug (see Dataflow expressions above).

### CFG-builder body-path lookup

- **PR #1786 — cache the per-body `TreePath` lookup in `CFCFGBuilder` (June 2026).**
  `CFGTranslationPhaseOne.process(CompilationUnitTree, UnderlyingAST)` (line ~527)
  computed the body's path with an *uncached* `trees.getPath(root, code)` — a JDK
  `Trees.PathFinder` `TreeScanner` that allocates a `new TreePath` per node visited
  while searching from the compilation-unit root down to the body, **once per method
  / lambda / initializer body**. For the *k*-th body in a file it re-scans the
  preceding *k*−1 bodies, so cost is **quadratic in bodies-per-compilation-unit**.
  This was the single largest `TreePath` allocator: on a full `checknullness`
  `--no-daemon` trace, `CFGTranslationPhaseOne.process` was the nearest-CF frame for
  **70%** of `com.sun.source.util.TreePath` allocation samples (2146 of 3057), vs. the
  per-tree path extension at `CFGTranslationPhaseOne.scan` (line ~562) at only
  **0.56%** (17 samples) — see the rejected "lazy path stack" note below.

  **Fix.** `CFCFGBuilder.build` already holds `checker` and `atypeFactory`, so it
  owns the checker's shared `TreePathCacher` (the same instance the `AnnotatedTypeFactory`
  populates during visiting). Replace the uncached search with
  `checker.getTreePathCacher().getPath(root, underlyingAST.getCode())` and feed the
  result into the existing `process(TreePath, UnderlyingAST)` overload. The cacher
  serves the enclosing class/method prefix from cache (warmed by visiting) and caches
  each node's path once, collapsing the per-body re-scan from O(bodies × file) toward
  O(file). **No class move is needed** — the framework-side caller already has the
  cache; the dataflow `CFGBuilder.build` standalone-tool path (used by `CFGProcessor`,
  which has no checker) is left on `trees.getPath`. This is a ~6-line change at one
  call site, no dataflow API change. It only does `getPath(root, realBodyTree)`
  lookups against the real AST keyed by tree identity, so none of the artificial-tree
  / bulk-population hazards of caching from the per-tree CFG traversal apply.

  **Measured allocation (deterministic `ThreadMXBean.getThreadAllocatedBytes` via JFR
  `ThreadAllocationStatistics`, single forked `javac`, one class of *N* trivial
  generic-call methods).** The win scales with methods-per-file, exactly as the
  quadratic predicts:

  | methods / file | master | cached | reduction |
  | --- | --- | --- | --- |
  | 100  |    524 MB |    506 MB |  −3.5% |
  | 300  |  1,453 MB |  1,251 MB | −13.9% |
  | 600  |  3,525 MB |  2,737 MB | −22.4% |
  | 1500 | 15,192 MB | 10,193 MB | −32.9% (wall −6.5%, 46.3 → 43.3 s) |

  On the all-systems corpus (267 *tiny* files, 1–3 bodies each) the effect is **~0%
  (noise)** — there is no per-file body reuse to exploit. On the realistic CF build the
  site is ~1.4% of total allocation (70% of `TreePath`, which is ~2% of TLAB events),
  so a normal mixed codebase sees a low-single-digit allocation reduction; the large
  numbers are worst-case protection for machine-generated or very large single-class
  files, where it removes a genuine quadratic. Wall clock tracks allocation (only
  measurable where allocation is large). The shared cache now retains the body-prefix
  paths it builds (bounded by the compilation unit — the same eager-scan caching the
  cacher already does on `AnnotatedTypeFactory.getPath` fallbacks). Validated with
  `framework`/`dataflow`/`NullnessTest` and `alltests`.

- **PR #1788 — make `TreePathCacher` lazy and route `AnnotatedTypeFactory.getPath` through
  it (June 2026).** Builds on PR #1786 (already in master). PR #1786 routes one
  `TreePathCacher.getPath` lookup per method body; but the eager `getPath` caches a
  `TreePath` for *every* node it DFS-traverses to reach the target, so on a large class each
  body lookup also caches the preceding bodies' internal nodes — O(file-nodes) of needless
  cached allocation, even though only body trees are ever queried. This change (a) makes the
  cacher lazy: `scan` only pushes/pops a `currentStack` and allocates nothing, and
  `buildPathForStack` materializes only the root-to-target path once the target is reached;
  and (b) routes `AnnotatedTypeFactory.getPath`'s two non-heuristic call sites through the
  cacher so they share that lazy cache. Measured (deterministic harness): **−4.0%**
  allocation on all-systems and **−51.6%** on a 1500-method class; passes full `alltests`.
  Three findings each cost a wasted attempt:

  - **The two halves must ship together.** The lazy cacher *without* the `getPath` call-site
    rerouting is *worse* than the eager cacher (1500-method class: 12.3 GB vs. 10.2 GB),
    because the eager cacher's broad node-caching is exactly what warms the cache for the
    per-body lookups; going lazy removes that warming unless the `getPath` sites repopulate
    it. Hence both halves are in one change.
  - **The `getPath(TreePath, Tree)` overload's locality is load-bearing on large classes.**
    It scans `currentPath`'s subtree *first* and expands outward (it relies on
    `TreeScanner.scan(Iterable, P)` visiting the path leaf-first), so a target near the
    visitor path is found without rescanning the whole unit. A "simplified" variant that
    just delegates to `getPath(root, target)` is byte-identical on normal code (all-systems
    5979 vs. 5982 MB) but reintroduces a residual O(members) rescan at that site, and the
    gap to the full version *widens* with class size:

    | methods/class | full (locality scan) | simplified (delegate to root) | delegate overhead |
    | --- | --- | --- | --- |
    | 1500 |  4936 MB |  5407 MB |  +9.5% |
    | 3000 | 11805 MB | 13686 MB | +15.9% |
    | 6000 | 32076 MB | 39444 MB | +23.0% (+7.4 GB) |

    So the extra overload earns its keep: invisible to users (contained in `TreePathCacher`)
    and decisive only for very large or machine-generated single-class files.
  - **Do not narrow that overload to a subtree-only scan.** Replacing `super.scan(rootPath,
    target)` with `super.scan(rootPath.getLeaf(), target)` (and seeding/`put(null)` tweaks)
    *looks* cleaner but is wrong: it returns null for out-of-subtree targets and caches that
    null as "absent from the unit", crashing type-argument inference on
    `all-systems/TypeVarVarArgs.java`. It passes `NullnessTest`, so it is not caught there;
    `framework/util/TreePathCacherTest` guards it directly (the `secondOverloadFindsOut...`
    case fails on the subtree-only variant). The original outward-expanding scan is correct
    and is now documented in the code.

- **PR #1789 — linear (instead of quadratic) `getPath` searches (June 2026).** Even after #1786 +
  #1788, a single class with very many methods allocated *super-linearly* (6000-method class: 32 GB,
  ~2.5–2.7×/doubling). A nearest-CF-frame allocation capture on a `gen-sized-program.py` size sweep
  traced **57% of allocation at 6000 methods** to `com.sun.tools.javac.util.List$2` iterators that
  `TreeScanner` allocates while `TreePathCacher.scan` *traverses* the tree — i.e. `getPath` searches
  rescanning the whole class (**268M node visits** at 1500 methods). Instrumenting `getPath` showed
  the targets were almost always local; they were just searched from too broad a start. Three causes,
  all fixed by starting each search from the tightest known path:
  - `AnnotatedTypeFactory.getPath`'s final fallback searched from `visitorTreePath` climbed up a
    *fixed two levels*; for a method-body path that overshoots to the **class**, forcing a whole-class
    rescan. It now searches from the original (tightest) `visitorTreePath` (the second overload still
    expands outward for non-local targets). Alone this cut traversal 268M → 38.5M (−86%).
  - `GenericAnnotatedTypeFactory.performFlowAnalysis` pinned `visitorTreePath` to the enclosing
    *class*; flow-analysis-time inference lookups now run against the **body** being analyzed.
    (A no-op by itself — the climb above negated it — but needed together with the first fix.)
  - `CFCFGBuilder`'s per-body `getPath(root, code)` scanned from the compilation-unit root (O(members)
    per body). `analyze` now primes that body's path in O(1) from the class path (`class → method →
    body`, an unambiguous extension; methods only — lambdas/initializers fall through), so the lookup
    is a cache hit.
  Result: per-method allocation went from *rising* (3.3 → 5.4 MB/method, quadratic) to *flat*
  (2.6 → 2.5 MB/method, linear); 6000-method class **32.1 GB → 14.8 GB (−54%)**, growing with size.
  **No effect on normal code** (all-systems unchanged) and **no correctness risk**: all three are
  search *hints* — `getPath` always returns the correct path (guarded by
  `framework/util/TreePathCacherTest`'s JDK-equivalence check). Validated with `alltests`.

### Value Checker

- **PR #1647** — *Cache a frequent conversion in the Value Checker.*
  `ValueAnnotatedTypeFactory.convertSpecialIntRangeToStandardIntRange`
  cached per `AnnotationMirror`; the unbounded-call profile flattened.

### Visitor and checker reviews

- **PR #1703** — *Small visitor performance tweaks.* Touched
  `BaseTypeVisitor` heavily (195 lines), `NullnessNoInitVisitor`,
  `InitializationVisitor`, and `AnnotatedTypeFactory`. Includes the
  hoisting of `getReceiverType` into a local, lint-option caching
  in `NullnessNoInitVisitor`, and removal of a duplicate null check
  in `checkMethodInvocability`.

The per-package "Review of" PRs are systematic audits, each typically a
mix of perf, clarification, and small correctness fixes:

- **PR #1705** — Initialization Checker (`InitializationATF`,
  `InitializationFieldAccessTreeAnnotator`,
  `InitializationParentATF`, `InitializationTransfer`).
- **PR #1706** — Nullness Checker (`CollectionToArrayHeuristics`,
  `NullnessNoInitATF`, `NullnessNoInitTransfer`).
- **PR #1708** — javacutil (`Resolver`, `TreeUtils`,
  `TreeUtilsAfterJava11`, `TypeAnnotationUtils`, `UserError`,
  `trees/TreeBuilder`, `trees/TreeParser`).
- **PR #1711** — framework (`DependentTypesHelper`,
  `ElementAnnotationUtil`, `TargetedElementAnnotationApplier`,
  `AbstractTypeInformationPresenter`, others).
- **PR #1716** — typeinference8 (`UseOfVariable`, `Variable`,
  `VariableBounds`, `Java8InferenceContext`, `Resolution`).
- **PR #1718** — framework/stub (`AnnotationFileElementTypes`,
  `AnnotationFileParser`, `AnnotationFileUtil`,
  `RemoveAnnotationsForInference`, `StubGenerator`).
- **PR #1719** — framework/type. Includes the `IPair`-sharing
  optimization across `SubtypeVisitHistory` and
  `StructuralEqualityVisitHistory`: a new package-private
  `putKey`/`removeKey`/`containsKey` API lets
  `StructuralEqualityVisitHistory` build one `IPair` per public call
  and pass it to both inner histories, halving the per-call `IPair`
  allocations on this hot path.
- **PR #1720** — framework/util/element (`ElementAnnotationUtil`,
  `IndexedElementAnnotationApplier`, `ParamApplier`,
  `TypeParamElementAnnotationApplier`, `TypeVarUseApplier`).
- **PR #1721** — common/basetype (`BaseTypeValidator`,
  `BaseTypeVisitor`). Includes the null-pointer guard in
  `checkAccessAllowed` for static fields with `@Unused`, the
  `Long`→`long` autoboxing fix in `checkSlowTypechecking`, the static
  `EnumSet` rewrite of `validateWildCardTargetLocation`, the
  `anyQualHasTargetLocations` short-circuit, and the empty-list
  early-out in `maybeReportAnnoOnIrrelevant`.
- **PR #1723** — common/value (`JavaExpressionOptimizer`,
  `ReflectiveEvaluator`, `ValueQualifierHierarchy`, `util/Range`).
- **PR #1724** — framework-test (`TestUtilities`, `TypecheckExecutor`,
  `TypecheckResult`, `TestDiagnostic`, `TestDiagnosticUtils`).
- **PR #1725** — type annotators (`LiteralTreeAnnotator`,
  `PropagationTreeAnnotator`, `DefaultForTypeAnnotator`,
  `DefaultQualifierForUseTypeAnnotator`, `ListTypeAnnotator`).
- **PR #1727** — framework/util/defaults (`Default`,
  `QualifierDefaults`).
- **PR #1763** — *Mixed performance tweaks.* `AnnotationFileParser`: skips JavaToken
  retention for JDK stubs via a new `parseStubUnitForJdk()` path (user stubs still use
  the full diagnostic-quality parser). `DefaultQualifierForUseTypeAnnotator`: added an
  empty-set early-out before `addMissingAnnotations` and canonicalized empty results to
  the shared `AnnotationMirrorSet.emptySet()` sentinel, avoiding a retained backing
  `ArrayList` per cached element. `QualifierDefaults.shouldBeAnnotated`: hoisted repeated
  `getKind()` calls into a local.

- **PR #1797 — `FoundRequired` lazy type formatting (June 2026).** `BaseTypeVisitor.FoundRequired`
  previously computed `ATM.toString()`/`toString(true)` eagerly in the constructor, paying full
  ATM-formatting cost even when the reported error would be suppressed by `@SuppressWarnings` or
  `-AsuppressWarnings`. Changed `found`/`required` fields from `String` to `Object` with anonymous
  inner classes whose `toString()` evaluates the ATM format lazily; `shouldPrintVerbose` result is
  memoized in a `verboseComputed`/`verbose` pair shared across both objects, so it is called at most
  once regardless of which field is stringified first. Also lazified the concatenated
  parameter-name prefix string in `checkMethodInvocabilityError`. CF's own sources suppress
  thousands of warnings, so the deferred cost is often zero. Wall-clock impact is proportional to
  suppression rate on the profiled workload; on the `checkNullness` build of CF itself the delta
  is within the noise floor (see A/B note in the name-decoding narrative below).
- **PR #1797 — `SourceChecker.shouldSkipUses` cache (June 2026).** Previously called
  `typeElement.toString()` (a `Symbol.toString()` → `Name.toString()` UTF-8 decode) and matched
  against a compiled regex on every invocation. Added an `IdentityHashMap<Name, Boolean>` cache
  keyed on `typeElement.getQualifiedName()` (identity-stable within one compilation's name table):
  first-visit still decodes and matches, but repeat visits for the same enclosing class are O(1).
  Reduces the `utf2string` attribution to cache-miss-only (4 samples in the post-fix profile).
- **PR #1797 — `Variable.computeHashCode` and `ProperType.computeHashCode` avoid `toString()` (June 2026).**
  `Variable.computeHashCode` hashed `elt.getSimpleName().toString()`, decoding the byte-backed `Name`
  on every hash computation. Changed to `elt.getSimpleName().hashCode()`, which returns the
  byte-table `index` (content-stable via interning, no decode). `ProperType.computeHashCode` hashed
  `properType.toString()` — an `ATM.toString()` call on every type-inference cache lookup. Replaced
  with `TypeKind.hashCode() + 31 * elt.getSimpleName().hashCode()` (element extracted for
  `DeclaredType` and `TypeVariable`; other kinds hash by kind alone). The new `ProperType` hash is
  weaker (no package component), but hash collisions only affect map distribution, not correctness
  (`equals` is unchanged).

### Correctness fixes adjacent to the perf work

These belong with the campaign because they were uncovered while
auditing the same files.

- **PR #1689** — *Preserve invariant `!isRunning => currentNode == null`
  even on exception.* Restores invariant in `AbstractAnalysis` finally
  block.
- **PR #1690** — *Change `catch Throwable` to `catch Exception`* in
  several framework call sites. `Throwable` accidentally suppressed
  things like `OutOfMemoryError` and `ThreadDeath`.
- **PR #1765** — *`ElementUtils.hasParameters` name-form fix.* Replaced
  `Class.getName()` (JVM binary form: `"java.util.Map$Entry"`) with
  `getCanonicalName()` (source form: `"java.util.Map.Entry"`) when matching
  against `TypeMirror.toString()`. Previously, nested classes and array types
  could be silently mismatched. Surfaced during the performance review sweep.

### Type-computation caches and declaration-tree lookup (June 2026)

The methodology, full A/B numbers, and the rejected variants are in the value-semantics narrative
under "Short list"; this is the canonical applied summary.

- **PR #1777** — *`methodAsMemberOf`, `directSupertypes`, and `elementType` caches.* Three caches on
  the `getAnnotatedType` hot paths, each storing/returning deep copies (ATMs are mutable):
  (1) **`methodAsMemberOfCache`** memoizes the `(method, receiver-type)`-determined substitution base
  inside `methodFromUse` (skips the declared-`@Poly*` guard; `Value`/`MethodVal` opt out via
  `shouldCacheMethodAsMemberOf`); (2) **`directSupertypesCache`** memoizes `directSupertypes(type)` (a
  pure function of the type — no poly guard or opt-out needed); (3) **`elementTypeCache`** memoizes the
  fully-computed (post-defaults) `getAnnotatedType(Element)` result (cheap element-identity key,
  `shouldCacheElementType` opt-out). Structural keys use a cache-local `Types.isSameType` comparison
  (`IsSameTypeAtmComparer`), not the global `ATM.equals`. **Full-build warm-daemon A/B** (`./gradlew
  checknullness`): the `methodAsMemberOf`+`directSupertypes` caches ≈ −9% cold / −13% warm; `elementType`
  (Phase 1) ≈ −10% on its own. Validate ATM-producing caches with `alltests` *diagnostics*, never a
  recompute cross-check (substitution mints fresh captures, so identical results compare `isSameType`-
  unequal — see the narrative). PR #1778 forks the Java-8 `check*` tasks so these caches' retained heap
  no longer piles into the shared Gradle daemon.
- **`declarationFromElement`: scan the enclosing method subtree, not the whole compilation unit
  (applied, PR #1780).** `TreeInfo.declarationFor(sym, root)` scanned the whole CU per local/
  parameter to find its declaration tree — JFR-attributed at ~13% of a `checknullness` compile.
  Replaced with `TreeInfo.declarationFor(sym, trees.getTree(elt.getEnclosingElement()))` (scan only the
  enclosing method/class), with a fallback to the full-CU scan, plus a short-circuit returning `null`
  for `TYPE_PARAMETER` (it scanned the whole CU only to return null). Same-session traced A/B:
  `declarationFromElement` −33%, **total on-CPU −5.1%**. Key distinction from the rejected
  `trees.getTree(localVar)` variant: `trees.getTree` on the *enclosing method* is cheap (position-based),
  whereas on the *local itself* it internally scans.
- **PR #1791** — *Per-CU `IdentityHashMap` tree caches, one-pass declaration scan, pooled copier map.*
  Three changes that remove cache thrash and redundant recomputation on large compilation units:
  (1) **LRU → `IdentityHashMap`.** `classAndMethodTreeCache`, `fromExpressionTreeCache`,
  `fromMemberTreeCache`, `fromTypeTreeCache`, and `elementToTreeCache` were bounded
  `CollectionsPlume.createLruCache(2048)` maps. On a large CU the live tree set overflows 2048, so the
  LRU evicts still-needed entries and re-`getAnnotatedType`s them — each miss recomputes *and*
  `deepCopy()`s the ATM (`classAndMethodTreeCache.put(tree, type.deepCopy())`). Plain `IdentityHashMap`s
  (Tree/Element keys are identity-compared anyway) remove the eviction thrash; they stay bounded in
  practice because `setRoot` already clears all five per compilation unit, so peak size is one CU's tree
  count, not the whole build. This swap alone is the bulk of the win.
  (2) **`DeclarationScanner` (extends PR #1780).** Rather than `TreeInfo.declarationFor(sym,
  enclosingTree)` per local/parameter, the first lookup into a given enclosing method/class scans that
  subtree once and records every variable/method/class declaration into `elementToTreeCache`; a
  `scannedEnclosingTrees` identity set (also cleared in `setRoot`) makes the scan once-per-subtree so
  sibling lookups hit the cache. Falls back to the full-CU `TreeInfo.declarationFor` if the scan misses.
  (3) **`AnnotatedTypeCopier` map pool.** `visit` allocated a fresh `IdentityHashMap` per copy; it now
  borrows a thread-local pooled map (cleared after use, fresh-map fallback if re-entrant).
  **Deterministic allocation A/B** (single forked `javac`, `jdk.ThreadAllocationStatistics`; see
  Reproducing measurements): total bytes allocated **−14.5% / −17.1% / −19.2%** on 300- / 600- /
  1500-method single-class files and **−6.6%** on an 80-file (15-method) corpus — the win grows with
  per-CU size, since that is when the 2048 LRU thrashes. The LRU→`IdentityHashMap` swap on its own is
  −10% to −13.5%; the scanner and copier pool add the rest. **Wall clock is roughly neutral** (≈ −3% at
  1500 methods, within noise) on heap-generous single-file compiles — these are not GC-bound, so the
  reduced allocation does not shorten them; the payoff is GC pressure / memory headroom, most relevant
  under default heap on a many-CU warm-daemon build. *Build/measure caveat:* flipping sides with `git
  stash` does **not** reliably recompile — `:framework:compileJava` reports `UP-TO-DATE` and serves the
  other side's classes — so force `--rerun-tasks` and gate each run by decompiling the shipped
  `checker/dist/checker.jar` (e.g. count `createLruCache` call sites in `AnnotatedTypeFactory`: 9 on
  master, 4 with this change). An un-gated early A/B read this change as ~0%, a false negative from two
  stale shadowJars. Dropped from the original proposal as separately risky and *not* part of the
  allocation win: a never-cleared `IdentityHashMap<AnnotationMirror, QualifierKind>` in the qualifier
  hierarchies (unbounded over a whole build) and disabling the per-CU
  `defaultQualifierForUseTypeAnnotator.clearCache()` (cross-CU staleness — the cache reads element
  annotated types that stubs/ajava refine).
- **`declarationFromElement` fallback: scan the enclosing subtree, not the whole CU; walk the
  enclosing chain (PR #1793, June 2026).** Two refinements to the variable/
  parameter path's *fallback* (the case PR #1791's `DeclarationScanner` left on the full-CU scan —
  when the scan missed or `shouldCache` is off). (1) **Subtree fallback.** When `elementToTreeCache`
  has no entry, try `TreeInfo.declarationFor(sym, enclosingTree)` (scan only the enclosing method/
  class subtree) *before* the full-CU `TreeInfo.declarationFor(sym, root)`. (2) **Enclosing-chain
  walk.** Master took `trees.getTree(elt.getEnclosingElement())` once and, if it was null, fell
  straight to the whole-CU scan; the change walks `getEnclosingElement()` upward until a non-null
  tree is found, so the subtree scan applies (and the `DeclarationScanner` gets primed) even when the
  immediate enclosing element has no tree. Both `declarationFor(sym, enclosingTree)` and
  `declarationFor(sym, root)` match by symbol identity, so the returned tree is unchanged — the only
  difference is how many tree nodes the scan visits. **Deterministic A/B** (single forked `javac`;
  size sweep of `gen-sized-program.py`, drift-controlled interleave — see Reproducing measurements):
  **wall-clock −11% / −15% / −26%** on 300- / 600- / 1500-method single-class files, **neutral at
  ≤100 methods** (3.2–4.5 s, JVM-startup-dominated) and on a 30-method file. **Allocation is flat**
  (`jdk.ThreadAllocationStatistics` within the ~0.3% run-to-run band at every size) — this is a
  scan-node / CPU change, not an allocation change. The win is super-linear in CU size and zero on
  typical small files: it is **worst-case protection** for large or machine-generated single-class
  compilation units (where master's full-CU fallback scan is super-linear in CU size), never a
  regression. Same flattening signature as the PR #1786 body-path quadratic; like that one it is
  invisible on the tiny-file all-systems corpus and only a size sweep exposes it.

### Tree-search quadratics: `declarationFromElement`, varargs arrays, and warning paths (PR #1803, June 2026)

Three independent O(n²)-in-compilation-unit-size scans, each a per-element or per-message
`Trees.getTree` / `getPath` / `TreePathCacher` search that rescanned the whole enclosing class
or compilation unit. All three are **worst-case protection** — super-linear only on large or
machine-generated single-class files, or on message-dense code — found by a shape × size ×
checker sweep (`gen-sized-program.py` `{generic,vararg,deep-nesting,many-fields}` ×
N=300/1000/3000 × `{nullness,interning,value}`) and each confirmed by instrumenting the
scanner's node-visit count: **nodes-per-`getPath` growing with N** is the signature (e.g. the
varargs case scanned ~6,400 nodes/call at N=300 and ~31,600 at N=1500 — the whole unit each time).

- **`declarationFromElement` member/variable lookup via the visitor path.** Even after
  PR #1791/#1793's `DeclarationScanner` and subtree fallback, `declarationFromElement` still
  called `trees.getTree(elt)` (member) and `trees.getTree(enclosing)` (a variable's enclosing
  method), and javac implements `Trees.getTree(Element)` as a `TreeInfo.declarationFor` scan of
  the enclosing class — O(class) per call, O(class²) across a class's members. (The in-code
  comment claiming `getTree` on a method is "position-based cheap" is wrong; it scans.) Fix:
  obtain the enclosing method/class tree from the factory's `visitorTreePath` (already set to
  the method body during flow analysis — the path through which these lookups arrive) instead of
  `trees.getTree`; the path is only a search-start hint, so the result is unchanged. Also fixed
  `DeclarationScanner` to cache by the raw `JCTree.sym`, not `TreeInfo.symbolFor`'s
  `baseSymbol()` — under `baseSymbol()` a generic method or a parameter is stored at a key no
  lookup ever uses. Full-build `checknullness` JFR: `declarationFromElement` 8.4% → 1.5%
  inclusive, `DeclScanner.scan` 200 → 21 samples; warm-daemon wall ~1m51s → ~1m43s (~7%, median
  of ≥3 reps/side, `shadowJar` rebuilt per side); a 1500-method single class 21.95s → 11.56s
  (−47%). `alltests` passes.

- **Varargs synthetic-array path.** `checkVarargs` → `getAnnotatedTypeVarargsArray` computes the
  type of the synthetic `NewArrayTree` the CFG builder wraps a varargs call's arguments in. That
  tree is not in the compilation unit, so defaulting it (`QualifierDefaults.nearestEnclosingExceptLocal`
  → `getPath`) made `TreePathCacher` scan the whole unit to fail to find it — O(unit) per varargs
  call. (`CFGTranslationPhaseOne`'s `handleArtificialTree` registers the same tree's path, but that
  registration does not reach this consumer.) Fix: register the array's path under the call site —
  `setPathForArtificialTree(arrayTree, new TreePath(getPath(callTree), arrayTree))` — before typing
  it; the call tree's own path is cheap because the call is being visited. Nullness vararg-shape
  on-CPU samples 12,306 → 2,597 at N=3000 (growth ~20× → ~4× over a 10× size increase);
  `TreeScanner.scan` under `getPath` 1,016 → 10 at N=1500. Only heavily-defaulting checkers
  (nullness) were affected; interning and the Value Checker default little and were already flat.

- **`@SuppressWarnings` / precise-position path lookup.** Reporting a message calls
  `SourceChecker.shouldSuppressWarnings(tree)` (and `getSourceWithPrecisePosition`), which looked
  up the tree's path with `getTreePathCacher().getPath(currentRoot, tree)` — a scan from the
  compilation-unit root — for the suppression walk. O(unit) per reported message, so a
  message-dense checker is quadratic in file size. The Interning Checker reports on every `==`, so
  a 3000-comparison file spent ~46% of on-CPU samples scanning for paths and grew ~6.6× over a 10×
  size increase (nullness, which does not report on those expressions, was unaffected). Fix: new
  `SourceChecker.pathToTree`, which starts the search from `visitor.getCurrentPath()` (public on
  `TreePathScanner`; an ancestor of the reported tree, which is being visited) and falls back to
  the root scan otherwise — same path result, local search. Interning generic-shape on-CPU samples
  1,567 → 715 at N=3000, growth ~6.6× → ~2.9× (linear). **This helps any checker that emits many
  messages, not just interning.**

A unifying lesson: a per-element or per-message `Trees.getTree` / `getPath` that re-derives a
tree's position scans the enclosing class or whole unit, which is super-linear when the caller
iterates members or messages. The fix is to **reuse position context the program already has** —
the visitor path, or a registered path for a synthetic tree — so the lookup localizes, rather
than to add another cache. Diagnostic caution: the JDK `TreePath.getPath(path, target)` "is it
under this path?" check is unreliable here — it searches the whole compilation unit of `path` and
returns non-null if `target` is found anywhere, not only under the leaf, so it does not confirm
locality. Instrument the cacher's node-visit count instead.

A post-fix verification sweep (the same shape × size × checker matrix) confirmed every
shape is linear/sublinear across all three checkers, with the top leaves at N=3000 being
irreducible framework work (`HashMap`/`IdentityHashMap` lookups, `AnnotatedTypeScanner`) rather
than tree scans. The sole remaining super-linear shape was `deep-nesting` (typeinference8),
addressed by PR #1805 below.

### Java 8 type-argument inference: work budget and fixpoint skip (PR #1805, June 2026)

The `deep-nesting` shape's super-linearity is Java-8 type-argument inference. Incorporating
bounds to a fixed point (`BoundSet.incorporateToFixedPoint` →
`VariableBounds.applyInstantiationsToBounds`) re-applies instantiations to every inference
variable on every iteration: O(iterations × variables × bounds) ≈ O(depth³) for a depth-D
nested-`id` chain. depth-80 in one method did not finish in 25 minutes. PR #1805 has three parts:

- **Work budget.** (PR #1829 later lowered this default to 10000 from a measurement of real code,
  made it configurable with `-AinferenceWorkBudget`, and reshaped/moved the regression test — see the
  PR #1829 section.) A per-invocation counter (`Java8InferenceContext.MAX_INCORPORATION_WORK`,
  originally 100k bound-visits) charged in
  `applyInstantiationsToBounds`. When exceeded, `recordIncorporationWork` throws
  `InferenceBudgetExceededError` (an `Error`, so it unwinds past the `catch (Exception)` /
  `catch (FalseBoundException)` blocks in the incorporation/resolution machinery), caught in
  `DefaultTypeArgumentInference.inferTypeArgs`. Inference is abandoned soundly: a new
  `type.argument.inference.budget` error is reported pointing the user to supply explicit type
  arguments, and the return type is defaulted (as for an inference crash, via
  `InferenceResult.needsDefaultedReturnType()`) so checking continues. The error is distinct from
  `type.argument.inference.crashed` because exceeding the budget is a deliberate give-up, not a
  crash. Reaching the budget takes ~0.15 s warm per inference problem. Regression test:
  `checker/tests/interning/InferenceWorkBudget.java` (depth-60). NB: do not
  `@SuppressWarnings("interning")` in such a test — that is the *checker name* and suppresses all of
  the interning checker's output, including the framework error under test.
- **Skip fully-resolved variables (`allBoundsProper`).** Once every bound of a variable is a proper
  type the bounds cannot change (`ProperType.applyInstantiations()` is the identity, and the
  changed-gated instantiation detection cannot fire), so re-scanning them is wasted. A boolean
  maintained at the only three sites that mutate the bound map (`addBound`, `restore`, the rebuild)
  drops such variables from the per-iteration scan. Provably equivalent; validated with a temporary
  flag that recomputed the invariant from scratch on every call and threw on mismatch — 0 violations
  across all-systems and the full `alltests` suite. The gain grows with nesting depth (~9% wall /
  8% CPU at depth-20), the signature of cutting the cubic fixpoint toward quadratic.
- **Two micro-opts.** `BoundSet.hasInstantiatedVariable` replaces a `getInstantiatedVariables()`
  call that built a `LinkedHashSet` every iteration only to test `!isEmpty()`; and
  `ProperType.getErased()` caches a proper type's erasure (immutable) instead of recomputing it on
  every subtyping check.

### Java 8 type-argument inference: constraint set, dependency traversal, and hashCode caching (PR #1813, June 2026)

Four independent optimizations to the `typeinference8` inference engine, all confined to the
`typeinference8` package. Measured on single-run cold-JVM wall clock (`gen-sized-program.py
--shape deep-nesting`, N methods each with 20 nested `id()` calls):

| N (methods) | master | PR #1813 | reduction |
| --- | --- | --- | --- |
| 30 | 25.0 s | 20.0 s | −20% |
| 80 | 55.8 s | 47.9 s | −14% |
| 100 | 72.0 s | 62.0 s | −14% |

(Single runs; these are cold-JVM wall-clock, not deterministic allocation A/Bs. The win is real
but the exact percentages carry ~±5% single-run noise.)

- **`ConstraintSet`: `ArrayDeque` + `HashSet` for O(1) deduplication.** The backing `ArrayList`
  made `add`, `push`, and `contains` O(n) (linear scan). Replaced with an `ArrayDeque` (preserves
  LIFO/FIFO order and `addFirst`/`addLast`/`descendingIterator`) plus a parallel `HashSet` for
  O(1) membership tests. All mutation sites — `add`, `push`, `pushAll`, `pop`, `remove` — now
  keep both structures in sync. `addAll` was also fixed: the old `list.addAll(constraintSet)` skipped
  the duplicate check (inconsistent with `add` and `push`); the new version deduplicates. The
  `remove(self)` case reallocates fresh structures instead of `clear()` to match the invariant that
  `fastLookup` and `list` are never independently partial.

- **`Dependencies.calculateTransitiveDependencies`: BFS replaces fixpoint.** The old implementation
  was an outer `while (changed)` loop that, on each pass, iterated all entries and `addAll`'d the
  transitive neighbours — effectively O(V²) or worse for dense dependency graphs. Replaced with a
  per-source BFS (`ArrayDeque` queue, `LinkedHashSet` as visited/reachable set): each variable is
  enqueued at most once, so the total work is O(V + E). On deep-nesting code where many inference
  variables have mutual dependencies this was a meaningful hotspot.

- **`hashCode` caching in constraint and type objects.** `TypeConstraint`, `Typing`, `Expression`,
  `InferenceType`, `UseOfVariable`, and `QualifierVar` each gain a `cachedHashCode` field (lazy,
  zero-sentinel pattern from PR #1812). These objects are effectively immutable post-construction
  and are placed in `HashSet`s / used as map keys during inference, so their `hashCode` was being
  recomputed on every lookup. `TypeConstraint.hashCode` delegates to `T.hashCode()` (an ATM hash),
  and `UseOfVariable.hashCode` chains five field hashes — both are non-trivial calls.

- **`VariableBounds.addQualifierBound` pre-filter.** Before calling
  `addConstraintsFromComplementaryQualifierBounds` and `addConstraintsFromComplementaryBounds`,
  the new code filters out qualifiers already present in `qualifierBounds.get(kind)`. If none are
  new, it returns immediately without entering the (potentially recursive) constraint-generation
  paths. This avoids redundant constraint proliferation when the same qualifier bound is added more
  than once (which happens during fixpoint iteration).

- **Javadoc guards on `Qualifier`, `QualifierTyping`, and `AbstractQualifier`.** Documents why
  these classes must not override `equals`/`hashCode`: the constraint solver relies on identity
  equality for `Qualifier` wrappers (value-based dedup would merge distinct constraints that happen
  to wrap the same annotation) and for `QualifierTyping` instances (multiple identically-shaped
  qualifier constraints must coexist). These are correctness comments, not perf changes; recorded
  here because they interact directly with the `ConstraintSet` `HashSet` deduplication above.

### Explorations that did not ship (June 2026, around PR #1805)

These were implemented and measured but kept out of #1805; recorded so they are not re-derived.

- **Incorporation worklist (the dependency-based variant of the `allBoundsProper` skip) — SHIPPED in
  PR #1829;** see the PR #1829 section below, which beat the ~3% recorded here by combining it with the
  constraint gating and measuring on deeper nesting, and replaced the verify harness with an
  always-on self-correcting rescan. The original deferral rationale follows. Replaced
  `allBoundsProper` with a per-`VariableBounds` `dirty` flag plus an *append-only, over-approximate*
  reverse-dependency list (`dependents`): `addBound` records edges β→α for each variable β mentioned
  in a new bound of α; when α is instantiated it marks its dependents dirty; `applyInstantiationsToBounds`
  skips a clean variable. Over-approximation makes it **safe by construction** — a stale edge causes
  only a harmless extra re-scan; only a *missing* edge is a bug, caught by the verify harness (after
  the worklist reports convergence, a full scan must find no change). **Correct** (0 verify violations
  across all-systems and the full `alltests` suite) and it eliminated the fixpoint's #1 self-time leaf
  (`applyInstantiationsToBounds` 17.8% → out of the top under `incorporateToFixedPoint`). **But the
  net wall-clock gain was only ~3% on inference-heavy synthetic code and <1% on realistic code**,
  because once the redundant rescan is gone the fixpoint's remaining cost is essential JLS-18 work
  (`ConstraintSet.applyInstantiations`/`reduceOneStep` ~9%) and javac `Symbol.apiComplete` (~8%) — the
  per-variable scan was the dominant *leaf* but not a large enough share of total time. **A second,
  separate benefit:** because the worklist does less work per depth, the budget triggers ~2× deeper
  (depth-60 completes; depth-100 aborts). That is a real *correctness/usability* win, not a downside —
  the budget abandons inference (a false positive on valid code), so raising the depth at which real
  generic code completes before the budget fires removes false positives. The depth-60 budget
  regression test then "fails", but that test only encodes the *old* limitation; the right response is
  to deepen it to a depth that still exceeds the budget — routine maintenance recording the higher
  capability, not a regression. Deferred for now (the user chose to ship the simpler `allBoundsProper`
  skip in #1805 first), but the case is stronger than the ~3% wall-clock alone: weigh the
  false-positive reduction too. Reconsider together with the constraint-reduction cost (below) —
  gating those by the same dirty flag is what could push the wall-clock gain past ~3%.
- **`InferenceType.applyInstantiations` list lazy-allocation.** It allocates three `ArrayList`s
  unconditionally and discards them when nothing is instantiated (the common case). A no-allocation
  pre-check was **measured neutral** (many allocations by *count*, negligible by *bytes* — the lists
  are empty/tiny; same lesson as `AnnotatedTypeScanner.markVisited`).
- **`getTargetType`, `asSuper`, `merge` caching.** `getTargetType` is called once per inference
  problem (no repeat to cache); `asSuper`/`merge` are intrinsic set-union/supertype work.

### Java 8 type-argument inference: incorporation worklist and tuned work budget (PR #1829, June 2026)

Ships the incorporation worklist that #1805 deferred (see "Explorations that did not ship" above),
this time with the constraint gating that makes it pay off, plus a self-correcting safety net and a
work budget re-tuned from a measurement of real code. Two parts.

**1. Self-correcting incorporation worklist.** The JLS-18 incorporation fixed point
(`BoundSet.incorporateToFixedPoint`) re-scanned *every* inference variable, and re-applied every
variable's constraints, on every round. The worklist re-scans only variables that can have changed:
each `VariableBounds` carries a `dirty` flag and an over-approximate, append-only reverse-dependency
set `dependents` (built in `addBound` from `AbstractType.getInferenceVariables()`); instantiating a
variable marks its dependents dirty, and `applyInstantiationsToBounds` skips a clean variable, which
also skips that variable's `constraints.applyInstantiations()` — so the constraint gating #1805's
note asked for falls out of the same skip, no separate flag.

The correctness guarantee is *not* a flag. When the worklist reports a fixed point,
`hasReachedFixedPoint` confirms it with one full un-gated rescan of every variable. At a true fixed
point that is a no-op; if the worklist ever skipped a variable (a missing reverse-dependency edge),
the rescan's `doApplyInstantiationsToBounds` has already applied the change, and the method marks all
variables dirty and runs another round. So the result is **identical to scanning every variable every
round, by construction** — the worklist is a pure optimization with no soundness knob to get wrong.
In production this self-heals silently; this project's tests run in **strict mode**
(`-Dcf.typeinference.worklist.strict`, set on all test tasks in `build.gradle`; `TypecheckExecutor`
runs the checker in-process so the property reaches it), where the same situation *throws*, turning a
worklist regression into a loud CI failure. **0 strict/self-heal events** across `alltests`,
`NullnessTest`, `InterningTest`, and a manual sweep of 425 `checker/tests/nullness` + 279
`checker/tests/index` files (all byte-identical to the pre-worklist baseline).

**Why it now beats #1805's ~3%.** #1805 measured the worklist alone, on shallow inference, at ~3%.
The gain is real on *deeply* nested generics, where the redundant rescan is cubic: wall clock
(`gen-sized-program --shape deep-nesting`, single-build `-D` toggle, interleaved, n=8) **−8.6% /
−11.4% / −19.0%** at nesting depth 8 / 12 / 20 — the win grows with depth. JFR (depth-8 × 800):
`doApplyInstantiationsToBounds` self-time 7.5% → 1.9% (3.8×), `ConstraintSet.applyInstantiations` off
the leaderboard, total on-CPU −6.5%; `reduceOneStep` (~54%, the essential JLS-18 constraint
reduction) is correctly untouched.

**The self-correcting rescan is free — but measure it in isolation.** On shallow generics and on a
realistic full `checknullness` the change is neutral. The first A/B (worklist branch vs. master, warm
daemon) read +0.5% — *within noise but the wrong comparison*: it conflates the worklist's savings
with the rescan's cost, which cancel on ordinary code, so a real rescan cost could hide behind a
worklist win. The right isolation is same-build, self-heal **on vs. off** (a temporary
`-Dcf.typeinference.worklist.noselfheal` plumbed to the `checknullness` fork; toggle verified with a
one-shot marker before trusting the result): 105.0 s vs. 107.0 s warm median — i.e. within noise (a
rescan cannot be negative-cost). It is free because each trivial inference problem's rescan is one
pass over a 1–2-variable bound set, and inference is a small slice of a build.

**2. Configurable, lowered work budget.** `Java8InferenceContext.MAX_INCORPORATION_WORK` was a
hardcoded 100000. Add `-AinferenceWorkBudget=N` to override it. The option is read once and cached on
the `AnnotatedTypeFactory` (`getInferenceWorkBudget`): a `Java8InferenceContext` is created per
generic invocation (`DefaultTypeArgumentInference.inferTypeArgs`), so reading `getOption` there would
repeat the lookup on a hot path for a value that is constant per compilation.

The default is lowered **100000 → 10000**, from a measurement (instrument `recordIncorporationWork`
to track the peak per problem; run with an effectively-unlimited budget so nothing aborts): the
heaviest hand-written generics reach only **994** work units (Guava with the Nullness Checker; the
framework's own source 363; a deliberately tricky stress test 296). So 100000 was ~100× more headroom
than the ~1000 the #1805 comment cited (994 is post-worklist, so the worklist did *not* meaningfully
shrink Guava's peak), and let a single pathological invocation grind ~4–5 s before bailing; 10000
keeps ~10× headroom and bails in ~3 s. There is an irreducible ~2.4 s floor (parsing/attributing the
deep expression, which the budget cannot cap). **False-positive-clean at 10000:** zero budget errors
on Guava under the Nullness, Interning, and Index Checkers, on the framework's own `checknullness`,
and on the all-systems corpus.

**The budget is checker-independent per problem.** The threshold is dominated by Java-type bound
incorporation (~cubic in nesting depth, checker-agnostic); the qualifier bounds each checker adds are
a smaller, comparable factor. The Nullness Checker fires the budget *three times* (its three
subcheckers each run their own inference), but at the same per-problem nesting depth as the
single-system Interning Checker (the three identical diagnostics dedupe to one in the test harness,
which compares actuals as a set). A single invocation **never** blows up regardless of
type-parameter count — a method with 400 chained-bound type parameters called once does not fire —
because the cubic cost lives in the *chained dependency that nesting creates*, so the budget cannot be
triggered without deep nesting.

**Notes for future sessions.**
- *Re-measure deferred/rejected items against the current baseline and a maximal workload.* The
  worklist was "~3%, deferred" in #1805; combining it with the constraint gating and measuring on
  deep nesting (not shallow synthetic) turned it into −19%.
- *A throwaway inference test that does not compile silently measures nothing.* An early "Interning
  inference is harder than Nullness" reading was an artifact: the throwaway file's `public` class name
  did not match its filename, so javac errored before inference ran. Use a non-public class (or a
  matching filename) for throwaway inference probes, and confirm the budget actually fires.
- *jtreg `nullness/Issue1438` timeouts under `alltests` are environmental*, not a regression: the file
  compiles in ~7.5 s standalone (well under the 20 s jtreg limit) and is marginally *faster* with the
  worklist; the timeouts are parallel-agent contention during the full run.
- *Shaping a budget regression test:* google-java-format escalates one nested call per line, so a
  test's line count tracks the *number of nested calls*. "More type parameters" backfires (each filler
  argument gets its own line — a 5-type-parameter chain formatted to 84 lines). The compact form keeps
  the expression under 100 columns so the formatter leaves it on one line: a method heavy enough to
  fire at shallow depth (return type mentions its type variable three times, parameters are wildcards)
  with short names. `checker/tests/nullness/InferenceWorkBudget.java` (default budget) and
  `checker/tests/inference-budget/InferenceWorkBudget.java` (small `-AinferenceWorkBudget`) are the
  two regression tests; `checker/tests/nullness/Java8InferenceWorklistStress.java` exercises the
  worklist's dependency tracking across interacting inference features under strict mode.

---

## Tried and rejected

Bring new evidence before revisiting any of these — a JFR trace on a
workload not previously considered, or a measurement that contradicts
the prior finding. A fresh hypothesis is not new evidence.

- **`AbstractAnalysis.getValue(Node)` subnode gate — identity rewrite, reorder, and check-removal
  all measured-and-rejected (June 2026).** The subnode gate in `getValue` uses `Collection#contains(n)`
  (structural `Node#equals`), even though the `nodeValues` map it guards is an `IdentityHashMap` and
  "two Nodes can be .equals but represent different CFG nodes" (see `Node`). Three angles, all
  rejected:
  - **(a) Structural→identity for correctness — not a demonstrated bug, and regresses allocation.**
    *Precise statement of the issue:* `nodeValues.get(n)` is identity-keyed, so the structural match
    **never returns a different node's value** — it only changes the *gate decision* (return `get(n)`
    vs. `null`). The only possible defect is returning `n`'s **own** (possibly stale) value when `n`
    is not truly a subnode. (My earlier "returns the wrong node's value" framing was imprecise.)
    Instrumenting both gates, the structural one diverges on real code (132 `leak=true` cases on the
    all-systems corpus) **only for constant-valued `ClassNameNode`s** (static-call qualifiers such as
    `Collectors`), whose value is identical at every occurrence — a full diagnostic A/B
    (`-Dcf.structuralGate` toggling both gates in one build) showed **zero diagnostic difference**
    across all 269 all-systems files, and no targeted attempt (repeated derefs, loops, nested instance
    calls `x.f(x.g())`, `this` during construction) could make a refinable node take the colliding
    path. The identity gate also **regresses allocation** (it refuses values the structural gate was
    eliding, forcing recomputation): on the `cond` shape allocation rose 28 GB → 46 GB at depth 160.
  - **(b) Reorder (`get(n)` first, skip the check when null) — no win.** Behavior-preserving, but the
    value is non-null at the gate **99.3%** of calls on all-systems and **100%** on `cond` (callers
    query subnodes that already have values), so the null-skip almost never applies; allocation
    unchanged.
  - **(c) The subnode check's allocation cost is realistically negligible.** `getOperands()` allocates
    per visited node (`Arrays.asList` for ternaries, `new ArrayList` for method invocations).
    Removing the check entirely (incorrect; measures its total cost) saved only **~1.2%** on
    all-systems (5703 → 5636 MB) — but **~35%** on the deeply-nested-ternary `cond` shape (9803 → 6333
    MB at D=120), making it a *second* super-linear allocator on that shape alongside the #602 rebuild.
    So the check is cheap on real code and not worth optimizing; the deep-nesting cost only matters if
    nested-expression worst-case protection is wanted (then: a non-allocating `getOperands`, or an
    ancestry check via a parent pointer, would remove it without changing correctness).
  - **(d) The immediate-operands fast-path buys nothing.** The `immediate.contains(n)` fast-path (skip
    the `ArrayDeque` BFS when `n` is a direct operand) hits only **13.3%** on all-systems, **0%** on the
    method-call-heavy `repeat` shape, and 53% on `cond` — and an A/B against a unified BFS (always
    allocate the queue, check every node) is **flat within noise** on all three (all-systems 5710 vs
    5718; cond 9783 vs 9746; repeat 1921 vs 1935 MB). The single avoided `ArrayDeque` is negligible
    against the shared `getOperands()` allocations. So the two-phase structure is a simplification
    candidate (a unified BFS is equivalent and shorter), not a perf lever.

  A `// TODO` documenting all of the above is in `AbstractAnalysis.getValue`. Net: leave as-is; pursue
  identity only as deliberate defensive hardening (allocation cost accepted), and the allocation only
  via a non-allocating subnode test, never by dropping the guard.

- **`AnnotatedTypeCopier.visit` pooled-map clear ratchet (June 2026).** `IdentityHashMap.clear`
  is ~2.6% of `checknullness` self-time, ~74% of it from `AnnotatedTypeCopier.visit`'s
  `finally { map.clear() }`. The pooled map never shrinks, so one large copy (observed max 879
  entries; avg ~4 over 1.7M copies on all-systems) ratchets the table to ~2048 slots and every later
  small copy then `Arrays.fill`s that whole table. The mechanism is real, but discarding the map
  when it grows past its initial table (so later clears stay O(32)) was **measured neutral** on two
  realistic workloads (all-systems and 60 framework/type files), by both wall *and* user CPU. The
  reason: `IdentityHashMap.clear` is a cache-resident `Arrays.fill`, fast per call — it samples high
  by *count* (called 1.7M+ times) but the actual time saved by a smaller table is negligible (same
  lesson as the `AnnotatedTypeScanner.markVisited` array sizing). Don't pursue without a workload
  where a genuinely large table is filled enough times that the byte-volume, not the call count,
  dominates. **Follow-up (PR #1815):** the pool was subsequently removed entirely (always fresh
  allocation) — also measured neutral on cold-JVM wall clock, but removes the re-entrancy fallback
  complexity. See Applied optimizations → `AnnotatedTypeScanner` and visitor state.
- **Per-CU tree-defaults memoization in `QualifierDefaults` (June 2026).** A full prototype of
  short-list item #4 — a per-compilation-unit cache from `(scope element, pre-defaulting type
  structure)` to the defaulted type, keyed by a sound `AppliedDefaultsKey` (identity scope +
  structural type hash) with an `EqualityAtmComparer` subclass adding `isDeclaration()`, write-back
  on hit via a pooled `AnnotatedTypeReplacer`, `deepCopy` snapshots of key and value on miss,
  cleared per-CU in `GenericAnnotatedTypeFactory.setRoot`, with `-Dcf.defaults.noTreeCache` and
  `-Dcf.defaults.cacheStats` runtime flags. The premise was that `applyDefaultsElement` runs ~9
  full-tree scans per call (one per `Default` plus checked/unchecked), replaceable by 3 cheaper
  walks on a hit. The hit rate hypothesis held — **79.6% on nullness, 85.5% on interning** over
  all-systems, well above the predicted ~24% break-even and ~60% "worth it" thresholds — but the
  cache was **net neutral-to-worse on every axis** in a same-jar kill-switch A/B (all-systems / 269
  files, median of 3–5): allocation **+2.06%** (5407 vs 5298 MB, deterministic
  `ThreadAllocationStatistics`), wall **+0.9%** (25.19 vs 24.96 s), user CPU **+1.3%** (85.4 vs 84.3
  s). Why the high hit rate doesn't pay: the per-`Default` scans are *cheap in-place annotation
  mutations* (`DefaultApplierElement` walks the type and sets annotations, allocating little and
  short-circuiting), not expensive operations — so the "9 scans → 3 walks" trade is between things
  of similar cost. Meanwhile the cache machinery is pure overhead: an `AppliedDefaultsKey` + a
  structural hash walk on *every* lookup, a structural equality walk + a replacer walk on every hit,
  and **two `deepCopy`s per miss** (key snapshot + stored value). The ~20% miss tail's deep copies
  alone account for the +2% allocation. The earlier "value-key lookup ≈ one scan, so neutral"
  intuition was right empirically; the patch's reframing to "9 scans" overcounted the cost being
  saved. Don't revisit without first showing — by instrumentation, not reasoning — that the
  `DefaultApplierElement` scan itself (not its call count) is an allocation/CPU hotspot; today it is
  not.
- **Caching `getAnnotatedType` for expression/variable trees.** `getAnnotatedType(Tree)` caches
  only class and method trees (`classAndMethodTreeCache`); expressions recompute `fromExpression` +
  `addComputedTypeAnnotations` every call, and `CFAbstractTransfer.getValueFromFactory` (~19%
  inclusive) hits that path per node during flow analysis. This non-caching is **intentional and
  load-bearing**: an expression's annotated type depends on context (assignment context, capture,
  the in-progress flow store), so a cache would return stale/unsound types. The flow-stable subset
  (`fromExpression`'s structural result) is in principle cacheable but the cached value must be
  frozen and `deepCopy`'d on use, which offsets the saving; not attempted, and risky against the
  `AnnotatedTypeMirror` cache invariants CLAUDE.md flags.
- **Large method bodies / dataflow size.** A size sweep of one method with N local-variable
  declarations-and-uses (N=200/400/800) is **linear** in N (marginal cost per statement flat once
  the fixed ~8 s JVM+javac+nullness init is subtracted). No quadratic in method-body size; the
  CFG/dataflow fixpoint scales as expected.

- **Cache-boundary flips after freezing the masters — PARTIALLY SUPERSEDED (PR #1798).** The first cut
  rejected boundary flips wholesale: "the cache-return copy is load-bearing; the dominant consumers
  mutate the returned type, so a flip only moves the copy." **That was survivorship bias from the
  `BugInCF` flush** — the flush only enumerates the *mutating* consumers, not the read-only majority, so
  reasoning from it overcounts the cost. A direct measurement of the read-only fraction (65–88% for
  `getAnnotatedType(Element)`) and a deterministic allocation A/B then showed the **`elementTypeCache`
  and `classAndMethodTreeCache` flips DO pay (small): ~−1% on generic-call-heavy code, noise elsewhere**
  (NOT the −5.3% an earlier estimate suggested — never reproduced against the post-flip baseline). Both
  shipped in PR #1798 (see the foundation section). The lesson stands for three *other* flips that were
  tried and genuinely do not pay, because their consumer is *always* mutating: (a) the raw **Element
  boundary feeding the tree pipeline** — `TypeFromExpressionVisitor` → `addComputedTypeAnnotations`
  (defaulting + flow refinement + annotators) rewrites the whole type every time; (b) the **`methodFromUse`
  on-hit copy-elision** — type-argument inference (`typeinference8.Resolution.resolveWithLowerBounds`)
  mutates the method type in place; and (c) the **`directSupertypesCache` flip** — its dominant consumer is
  `AsSuperVisitor` (the cache exists *because* `asSuper`/`allSupertypes` recompute supertypes constantly),
  which mutates each returned supertype in place (`copyPrimaryAnnos`, `setUpperBound`, `fixupBoundAnnotations`
  via `visitDeclared_{Typevar,Wildcard,Intersection}`); flipping it (return the frozen masters, skip the
  per-hit `deepCopySupertypes`) flushed 60 `BugInCF`s, all in `AsSuperVisitor`, and fixing them would
  relocate the per-hit copy into `AsSuperVisitor` — fired once per asSuper walk-step, the same frequency —
  a provable wash. Reverted. Rule of thumb confirmed: **a post-pipeline cache whose hits are mostly
  read-only pays from a flip; a cache whose hot consumer rewrites the result in place does not — and you
  can tell which from whether the hot consumer (tree pipeline / inference / asSuper) is in the flush.**
- **Caching `AnnotatedTypeMirror.hashCode()` on frozen types (PR #1798 session).** The standing idea
  (the hash can't be cached *because* ATMs are mutable) is unblocked for frozen types — but
  instrumentation showed **0.0% of `hashCode()` calls land on frozen types** (every hot hash target is a
  mutable working copy, since the caches hand out copies). Worthless in the current architecture, and it
  would only become useful after a boundary flip that itself does not pay.
- **Shallow-location defaulting shortcut (PR #1798 session).** Skipping `QualifierDefaults`'s recursive
  descent for top-level-only locations (FIELD/PARAMETER/RETURN/RECEIVER/RESOURCE_VARIABLE/
  EXCEPTION_PARAMETER/CONSTRUCTOR_RESULT) is sound and cut scan calls **10.2%**, but those saved scans are
  over cheap shallow types and **allocation was flat** — negligible. The cost is the deep `OTHERWISE`/
  bound traversals over generic types; merging those into a single pass is a high-risk refactor with a
  ~2% ceiling (defaulting is single-digit-% of CPU). Distinct from the deferred Defaulting Phase 2
  (caching the result); this was the cache-free variant.
- **`AnnotationMirror → QualifierKind` second-level cache in qualifier hierarchies (June 2026).**
  `NoElementQualifierHierarchy.getQualifierKind(AnnotationMirror)` and the matching method in
  `ElementQualifierHierarchy` already use an `elementToQualifierKind: IdentityHashMap<TypeElement,
  QualifierKind>` (PR #1670) that resolves the kind in O(1) via a single identity probe on a tiny map
  (~3–5 entries for Nullness, ~15–20 for Value). Proposed: add a second-level
  `annoToQualifierKindMap: IdentityHashMap<AnnotationMirror, QualifierKind>` so that repeat queries
  for the same `AnnotationMirror` instance bypass the TypeElement extraction entirely.

  **The miss path that is being "saved" is already free.** For a `Attribute.Compound` annotation
  (the common case), `getAnnotationType().asElement()` reduces to two direct field reads
  (`anno.type.tsym`) — the same cost as an `IdentityHashMap` probe after JIT devirtualization.
  There is nothing to save: the second-level cache adds overhead on misses (an `instanceof` check
  plus a `put`) and is neutral on hits (trades one two-field-read path for one identity lookup),
  netting to zero or slightly negative.

  **A/B (deterministic `jdk.ThreadAllocationStatistics`, single forked `javac`):**

  | corpus | master | branch | delta |
  | --- | --- | --- | --- |
  | Nullness checker, 600-method class (`NoElementQualifierHierarchy`) | 1396.8 MB | 1397.9 MB | **+0.08%** (noise) |
  | Value checker, 300-method `@IntRange` class (`ElementQualifierHierarchy`) | 194.7 MB | 195.0 MB | **+0.15%** (noise) |

  Both within the ~0.5% run-to-run band. **Rejected on measurement: flat allocation, and for
  `ElementQualifierHierarchy` the map grows unboundedly** — the Value checker processes many
  distinct `@IntRange(from=X, to=Y)` instances whose `AnnotationMirror` identity is unique per
  value combination, so `annoToQualifierKindMap` accumulates one entry per distinct annotation
  instance seen over the whole build with no bound or clear. **General lesson: a second-level
  cache in front of an already-O(1) tiny-map lookup provides no benefit — the cost being
  "saved" is on the order of two field reads, below any measurable threshold.**

- **Keeping the `DefaultQualifierForUseTypeAnnotator` cache warm across compilation units (June 2026).**
  `GenericAnnotatedTypeFactory.setRoot` clears `defaultQualifierForUseTypeAnnotator`'s
  `elementToDefaults` (Element → default-for-use qualifiers) per compilation unit, alongside the other
  per-CU caches. Proposed: stop clearing it, so defaults computed for a library element (e.g.
  `java.util.Map`) in one CU are reused in later CUs. **The cache-hit win is real but a vanity metric —
  it does not move allocation or CPU.** Instrumented hit/miss counters (runtime-toggled with
  `-Ddqfu.noclear`) over a 120-file corpus that references many distinct JDK types: warm cuts misses
  from **18,396 to 2,331 (−87%)**, since the per-CU clear was flushing ~16.6k entries that each later CU
  re-missed. (A simpler corpus that mostly uses `Object`/generics shows almost nothing — 1,252 → 936 —
  so this needs a *diverse-library-type* corpus to exercise at all.) But the deterministic
  allocation A/B (`jdk.ThreadAllocationStatistics`) was **−0.2%, within noise** on both corpora, and
  on-CPU `ExecutionSample` count did not move (warm side nominally higher, inside ±5–10% sampling
  noise). The reason the misses are nearly free to recompute: `getDefaultAnnosForUses` already
  canonicalizes the overwhelmingly-common empty result to the shared `AnnotationMirrorSet.emptySet()`
  sentinel, so a miss on a type with no `@DefaultQualifierForUse` (≈ every JDK type) allocates only a
  tiny transient set that is immediately discarded — 16k misses ≈ 1 MB against a 2.2 GB total.
  **Rejected:** no measurable allocation/CPU benefit, and it trades away a correctness invariant — the
  cache reads element annotated types (`getExplicitAnnos` → `getElementAnnotations`) that stub/ajava
  loading can refine across compilation units, so a warm entry can go stale (independent of WPI). A
  checker that makes heavy use of `@DefaultQualifierForUse` (non-empty default sets) could show a
  different allocation profile and would be worth re-measuring before revisiting; for `NullnessChecker`
  the change is all cost, no measurable gain. **General lesson: a cache-hit-rate improvement is not a
  performance result. When the miss path is already cheap (here, an empty-set sentinel), eliminating
  misses changes neither allocation nor wall clock — always confirm a hit-rate gain on the deterministic
  allocation / on-CPU A/B before crediting it, and exercise element-keyed caches with a realistic,
  diverse corpus, since trivial synthetic inputs under-fill them.**
- **`AnnotatedTypeMirror.getEffectiveAnnotations` caching.** JFR-
  attributed self-time was ~0.05% on the alltests trace and ~0.1% on
  the Oscar EMR (~4000 file) trace. Not a hotspot.
- **Lazy path stack in `CFGTranslationPhaseOne.scan` (June 2026, during PR #1786).** `scan` eagerly does
  `new TreePath(path, tree)` for *every* tree it visits to maintain `getCurrentPath()`,
  and most of those paths are never queried (only `MethodInvocationNode`, 1 of 78 node
  types, retains one; the other 22 of 23 `getCurrentPath()` call sites feed
  `TreePathUtil` helpers that extract a fact and drop the path). The proposed fix: keep
  a `Tree` stack and materialize the `TreePath` lazily on `getCurrentPath()`, allocating
  nothing for unqueried trees. **Rejected as not worth it: the target is negligible.**
  Pure-counting instrumentation (no behavior change; it also *simulates* the lazy stack's
  allocation count) on the all-systems corpus measured **eagerAllocs = 11,665 (~373 KB)**
  for the whole 267-file run, of which the lazy stack would save 47.9% — i.e. ~178 KB
  against a ~6 GB total. The JFR agrees: `CFGTranslationPhaseOne.scan` (line ~562) is
  only **0.56%** of `TreePath` allocation (17 samples), ≈0.01% of total. The "70% of
  `TreePath` allocs in the CFG builder" headline is **not** this line — it is the body-path
  *search* at `process` / line ~527 (see the applied "CFG-builder body-path lookup" note),
  which caching fixes for ~6 lines instead of a risky rewrite of dataflow's central
  traversal.
- **Pre-sizing `AnnotationMirrorSet`'s backing array (June 2026, during PR #1785).**
  `AnnotationMirrorSet.<init>` was the 2nd-largest `Object[]` source in the `checknullness`
  worker (18.77%, 6,901 samples, behind only the visitor maps). But it is not oversized: the
  set is array-backed by `shadowList = new ArrayList<>(2)`, already a 2-element `Object[2]`.
  With compressed oops (the realistic-heap case) and 8-byte object alignment, `Object[1]`
  (20 B → padded 24 B) and `Object[2]` (24 B) cost the *same* 24 bytes, so shrinking to 1 saves
  nothing and would force a resize on every 2-element set (common: multi-hierarchy qualifier
  sets, declaration-annotation sets). `Object[2]` is already at the alignment floor while holding
  two without resizing; the 18.77% is allocation *volume* (one tiny array per set, and sets are
  created constantly), which a capacity argument cannot reduce. Cutting it needs *fewer* set
  instances — empty/singleton sentinels or a lazy/specialized backing store — which is an
  architectural, correctness-sensitive change to a hot `Set`/`DeepCopyable` path, not a size
  tweak. The empty-set case is already partly handled (the `emptySet()` sentinel and the
  `getAnnotations()` `isEmpty()` short-circuit).
- **`CFAbstractStore.copyMap` allocation avoidance.** `new HashMap<>(emptyMap)`
  and `new HashMap<>()` produce identical JIT output once the map is
  written to; the "savings" were illusory.
- **`clear()` on `AnnotatedTypeScanner.visitedNodes` in `reset()`.**
  Tried as an alternative to reallocating. An earlier note (written without
  fresh measurement data) claimed G1/ZGC makes `clear()` cheaper. Re-measured
  June 2026 on `allNullnessTests -PmaxParallelForks=1`: `IdentityHashMap.clear`
  consumed 3.42% of leaf-frame samples vs ≈1.27% net for reallocation. The
  pre-sizing from PR #1671 is what makes `clear()` lose: it enlarged the
  array to 128 slots that `clear()` must zero via an explicit Java loop,
  while TLAB allocation for the same array uses JVM bulk zeroing. See the
  applied section for the current measured numbers.
- **Converting `visitedNodes` from `IdentityHashMap` to `HashMap`.**
  Identity is required for correctness — distinct ATM instances
  representing the same Java type must be visited separately to break
  cycles. Identity is also faster.
- **Aggressively clearing `SubtypeVisitHistory` with `IdentityHashMap`
  keys.** An earlier attempt OOM'd or hung on some checker test
  inputs. The version that shipped (PR #1634, just prior to the window
  above) keeps the original `HashMap<IPair<ATM, ATM>>` and clears it
  at the start of each top-level subtype check.
- **`Collections.synchronizedMap(new IdentityHashMap<>())` for the
  qualified-name cache.** Considered for thread-safety paranoia;
  threading audit confirmed AT factories are confined to the javac
  main thread. Plain `IdentityHashMap` shipped instead, matching every
  other LRU cache on the same object.
- **`==` fast-path in `isSupportedQualifier` before the `Set` lookup.**
  Raised on the short list (after PR #1673 interned annotation names)
  on the theory that a reference-equality check would skip the hash
  computation entirely. Investigation showed the premise is wrong twice
  over. First, `String` caches its own hash code, so the hash is not
  recomputed across the repeated interned-name lookups on this path.
  Second, the backing set is *already* interned: it is built from
  `Class.getCanonicalName()`, and for the packaged top-level annotation
  types that qualifiers always are, the canonical name equals the binary
  name returned by `Class.getName()`, which the JVM interns. So
  `getSupportedTypeQualifierNames().contains(annotationName(a))` already
  matches by reference inside `String.equals`'s identity short-circuit;
  there is no slow `equals` to skip. An isolated JDK micro-benchmark
  (HotSpot 21) confirmed the current `HashSet.contains` of an interned
  key runs at ~2.7 ns/op, identical to an interned-set variant, while a
  linear `==` scan only edges it out below ~5 qualifiers and loses past
  that. The linear-scan variant also gives up the public `Set<String>`
  return type of `getSupportedTypeQualifierNames` and adds a correctness
  dependency on every caller passing an interned string. Not worth it.
- **`constructorFromUse` cache (analog of the `methodFromUse`/`asMemberOf` cache).** Implemented,
  validated correct (all suites pass), measured **flat-to-slightly-negative** despite a **96.4% hit
  rate**. The deep-copy-cache overhead floor (structural key hash + deep-copy on hit + deep-copy of
  the stored key ≈ 2 type-walks) roughly *equals* the work a hit saves, because the saved part is just
  the constructor `asMemberOf` (`getAnnotatedType(ctor)` is already element-cached) and constructors are
  infrequent (~5–10k calls). Lesson: hit rate is necessary but not sufficient — confirm with the
  wall-clock A/B. Revisit only if immutability removes the deep-copy tax. Full detail in the
  value-semantics narrative below.
- **Deferring polymorphic-qualifier resolution past the `methodFromUse` cache (to drop the `@Poly*`
  guard).** Clean design exists (route the hook by the cached per-element poly check; no per-call cost),
  but **payoff is negligible**: an instrumented `checknullness` found only **0.1% of cacheable method
  calls (322/250,000) are on poly-declared methods** — the guard already admits 99.9% of calls. Keep the
  guard. (Would help a checker whose calls are dominated by polymorphic methods; not the realistic target.)
- **`declarationFromElement` via `trees.getTree(localVar)`.** Verified to return the identical tree
  (8,124/8,124 match) but a **no-op**: `trees.getTree` for a local/parameter internally calls the same
  `TreeInfo$DeclScanner` — it relocates the scan, it does not avoid it. Lesson: verifying the *result*
  matches is not verifying the *cost* drops; confirm the expensive leaf disappears. (The fix that *did*
  work — scanning the enclosing method subtree — is in Applied optimizations.)
- **`declarationFromElement` via a single-pass declaration map.** Build a per-CU `element → VariableTree`
  map in one `TreeScanner` pass, replacing per-element scans with lookups. Correct after a fix, but
  **flat**: javac **defers attribution of lambda/generic-method bodies**, so the variables that dominate
  the cost have null symbols when the pass runs and are skipped; they later miss the map and fall back to
  the full scan (`DeclScanner` stayed 99.7% under `declarationFromElement`). Pre-population can't win —
  the expensive variables aren't attributed at any single build point.
- **Shrinking the new heavy caches (`directSupertypes` at `cacheSize/2`) to reclaim memory.** Measured a
  **~10% wall-clock regression** (it gave back essentially all of the `elementType`/Phase-1 gain) — far
  worse than its 90.5%→81% hit-rate delta suggested. The new caches' +50–70 MB retained heap is the price
  of the perf; the right way to cut it is reducing per-entry weight (immutability), not entry count. Keep
  all caches at full `cacheSize`.
- **`TypeKind` as a field on `AnnotatedTypeMirror`** — *superseded by PR #1763, do not
  implement.* The goal (avoid the heap hop through `underlyingType.getKind()`) is already
  met more cheaply: every subclass whose kind is constant (`AnnotatedDeclaredType`,
  `AnnotatedArrayType`, `AnnotatedExecutableType`, `AnnotatedTypeVariable`,
  `AnnotatedNullType`, `AnnotatedWildcardType`, `AnnotatedIntersectionType`,
  `AnnotatedUnionType`) overrides `getKind()` to return the constant inline (PR #1763) —
  zero memory and zero indirection, strictly better than the proposed ~8 MB field. Only
  `AnnotatedPrimitiveType` and `AnnotatedNoType` fall through to the base method, and for
  them `underlyingType.getKind()` is cheap and does not force symbol completion (see the
  doc comment on the base `getKind()`).
- **Dropping `MethodInvocationNode`'s `TreePath` field.** It is the only one of 78 `Node` types that
  retains a `TreePath` (captured cheaply from `getCurrentPath()` at CFG-build time), for two
  framework consumers — WPI's `isRecursiveCall` (`enclosingMethod`) and `AliasingTransfer` (the
  invocation's parent). Investigated June 2026 (PR #1788 session) as a memory save and found **not
  worth it**: CFGs are per-compilation-unit (`subcheckerSharedCFG` is cleared on `setRoot`,
  `flowResult` nulled), so the paths are transient, not retained program-wide. Reconstructing on
  demand (`atypeFactory.getPath(node.getTree())`) is feasible — both consumers hold the factory — but
  must preserve behavior for *synthetic* invocation nodes (desugared
  `iterator()`/`hasNext()`/`next()`/`close()`, which `AliasingTransfer` also visits). If ever touched,
  do it for decoupling (WPI could read the enclosing method from `CFGMethod.getMethod()` instead of
  walking the path), not for memory.
- **Equal-store short-circuit in the analysis store merge (explored for PR #1793, June 2026).** In
  `ForwardAnalysisImpl.mergeStores` (and the two merge sites in `BackwardAnalysisImpl`), check
  `newStore.equals(previousStore)` before calling `leastUpperBound`/`widenedUpperBound`, and reuse the
  existing store when equal — skipping the LUB, which allocates a fresh store and its five maps. The
  intent: avoid the throwaway LUB allocation at fixpoint when a merge does not change the store.
  **A/B (deterministic `jdk.ThreadAllocationStatistics` + wall clock; `gen-sized-program.py` and a
  loop-heavy variant, drift-controlled interleave of prebuilt jars):** allocation **−1.1% to −1.5%**,
  consistent across sizes and on the loop corpus — real but small. **Wall clock neutral-to-worse**:
  flat at ≤300 methods, **+4–5% on the loop-heavy 600-method corpus** (master ~59 s → ~62 s,
  interleaved). **Rejected: the allocation saving is below the wall-clock cost it adds.** The reason is
  structural and worth recording, because "skip the LUB when nothing changed" looks free but is not:
    - `CFAbstractStore.equals` already has an **O(1) size fast-path** (compare the five map sizes; the
      size-only `hashCode` matches) — so merges where the live-variable *set* changed are already
      rejected for free. The cost the short-circuit pays is the **same-size, different-value** case —
      the *dominant* case during loop fixpoint convergence, where the variable set is stable while
      abstract values refine. There `equals` must fall through to `supersetOf` and walk every entry.
    - On that case the short-circuit does a **double walk**: the failed `equals` walk, then the LUB
      walk it could not skip. Master does one walk. That extra per-merge walk is the wall-clock
      regression.
  **Alternatives explored, both dead ends:**
    - **`==` instead of `.equals` (reference identity).** Cheaper (a pointer compare, never a walk),
      but on the same Loop600 interleave allocation came back to **−0.2% (master, within noise)** — the
      equal stores at merge points are *distinct objects* (content-equal, not reference-equal), so `==`
      fires almost never and the allocation win vanishes. Wall clock stayed at master. Net: nothing.
    - **Fold the equality detection into the single LUB walk** (`upperBoundOrPrevious`: track during
      `upperBound`'s existing entry walk whether the result equals `previous`, and return `previous`
      when so). This removes the double walk — but `upperBound` still allocates `newStore` at line 1171
      before it can know the result, so it **loses the allocation saving** and only restores wall-clock
      parity with master. No net win, added complexity. The two goals are in tension: saving the
      allocation requires knowing equality *before* building `newStore` (a pre-walk = the short-circuit,
      with its double-walk tax), while avoiding the double walk requires building `newStore` first.
      Determining same-size equality *is* a full walk, of the same order as the LUB it would skip.
  **Revisit only with new evidence on a memory-bound workload.** The whole prize is ~1.4% allocation
  with no CPU win; on a heap-generous compile that is invisible. It could convert to a real win only
  under default heap on a many-CU warm-daemon build where GC pressure dominates (the regime where
  PR #1791's allocation cuts pay off) — measure there before reconsidering, not on single-file
  allocation totals. See the Short list for the one way to make the same-size `equals` cheap enough
  (a maintained content hash) and why it was not pursued.

- **`declarationFromElement` eager whole-class `DeclarationScanner` batch (PR #1803 session).**
  Before the visitor-path fix shipped (Applied optimizations), the first attempt located the
  outermost enclosing class and scanned it once with `DeclarationScanner` to cache every member's
  tree. It helped a 1500-method single class (~11%) but **regressed realistic `checknullness`:
  `declarationFromElement` 8.4% → 14.4% inclusive, warm wall 2m15s → 2m26s (~+8%)**. Cause: the
  eager scan recurses into every method body (once per each nullness subchecker factory) — overhead
  that does not pay off on realistic many-small-class CUs — and javac's symbol-identity instability
  (queries arrive with view / `baseSymbol` symbols, not the declaration symbol) means parameters
  and locals miss the batch cache and fall back to a class-level scan anyway. The visitor-path fix
  replaced it: it adds **zero** scans, so it helps large classes and never regresses small ones.
  Lesson reaffirmed: a giant-single-class A/B can read as a big win while the realistic full build
  regresses; always A/B the full build.

- **Pointing `visitorTreePath` at the inference expression during type-argument inference
  (PR #1803 session).** A hypothesis for the varargs `getPath` quadratic: set the factory's
  `visitorTreePath` to `pathToExpression` around `DefaultTypeArgumentInference.inferTypeArgs`.
  **No effect** — only ~8% of the hot `getPath` scans were under inference; the real sources were
  the synthetic varargs array and (for interning) the warning-report path, both fixed separately.

- **typeinference8 incorporation: removing redundant `applyInstantiations` work (two attempts,
  both measured-neutral, PR #1803 session).** Deeply nested generic invocations
  (`id(id(...id(x)))`) make Java-8 type-argument inference super-linear in nesting *depth*
  (`VariableBounds.applyInstantiationsToBounds` is ~14% self-time on the `deep-nesting` shape; a
  depth-80 single method does not finish in 25 minutes). (1) Computing `bound.applyInstantiations()`
  once instead of twice (the change-check, then the rebuild) was **neutral** — the fast-path
  already breaks on the first changed bound, so the redundancy was ~1 bound, not 2×. (2) Deleting
  the redundant per-iteration full apply-pass in `BoundSet.incorporateToFixedPoint` (the loop that
  re-applies to every variable before the per-variable loop) was **neutral** — those calls are
  individually cheap no-ops. The cost is **not** redundant work: 69% of self-time is in
  `applyInstantiationsToBounds`'s own loops over cheap `UseOfVariable` bounds, i.e. the O(depth³)
  *count* of (variable × bound × fixpoint-iteration) tuples the JLS-18 incorporation fixpoint
  processes. The only direction that could change the complexity is in the Short list.

- **`MethodInvocationNode.hashCode()`'s `Objects.hash` varargs allocation (June 2026).** `hashCode`
  uses `Objects.hash(target, arguments)`, the varargs `Object[]` antipattern called out in Applied
  optimizations. Raised as a possible hot-path allocation because `Node` hash/equals *could* back the
  dataflow worklists and stores. **Rejected — the structural override is never reached in production.**
  (1) Self-time: **0 of 14,812** `ExecutionSample`s across the realistic traces. (2) Static
  reachability: every `Node`-keyed map in the dataflow analysis — `AbstractAnalysis.nodeValues` /
  `syncedFrom`, the per-input analysis caches, `ForwardAnalysisImpl.storesAtReturnStatements`,
  `AnalysisResult.nodeValues` — is an `IdentityHashMap`, and the `Set<Node>` values
  (`treeLookup`/CFG-build) are `IdentityArraySet`; both use `System.identityHashCode`/`==` and **never
  call the structural `hashCode`/`equals`**. The only structural `Map<Node, _>` is
  `ConstantPropagationStore.contents` (`LinkedHashMap`) — the *example* constant-propagation analysis,
  not used by any production checker. So the varargs `Object[]` never allocates during real checking;
  there is nothing to optimize. Lesson: confirm a structural `hashCode` is actually invoked (the
  collection must be a structural, not identity, map) before treating it as an allocation source.

- **Meta-annotation read cache (June 2026, PR #1803 session).** A proposed cache memoizing
  `annotation.getAnnotationType().asElement().getAnnotationMirrors()` (e.g. an instance
  `IdentityHashMap<TypeElement, AnnotationMirrorSet>` in `AnnotatedTypeFactory`) to avoid
  re-reading an annotation type's meta-annotations. **Below the noise floor and rejected.** On
  the 99 MB `checknullness` worker trace (11,691 on-CPU samples): all `getAnnotationMirrors`
  meta-reads = 18 samples (**0.15%**); `getDeclAnnotationWithMetaAnnotation` (contracts/WPI) =
  0.14%; `getAnnotationWithMetaAnnotation` (the `CFAbstractStore` monotonic path, already guarded
  by `getSupportedMonotonicTypeQualifiers().isEmpty()`) = 0.03% — the same magnitude as the
  rejected `getEffectiveAnnotations` false-hotspot (0.05%). The only warm neighbor is
  `inheritOverriddenDeclAnnos` (2.59%), but its self-time is `overriddenMethods()` /
  `getDeclAnnotations` (`apiComplete`, `ElementFilter`, `Scope` walks), **not** the meta-read — so
  if this area is ever revisited, cache that path, not the meta-annotation lookup.

- **Copy-on-write / lazy-share for `CFAbstractStore` maps (June 2026, PR #1803 session).** A
  copy-on-write scheme for `CFAbstractStore` (in `framework.flow`, distinct from the ATM
  copy-on-write discussed in the Short list) — share the five backing maps between a store and its
  copy until a mutation forces `ensureUnshared()`. Conceptually valid but **not worth it for this
  workload.** On the `NullnessTest` slice (aggregated worker traces): `createCopiedStore` (the
  store-copy entry point) = **0.09%** CPU; the `CFAbstractStore` copy constructor is 2.45% of
  `HashMap$Node` allocation and `HashMap$Node` is 1.34% of total allocation, so store-copy is
  **<0.1% of total allocation**. (A "copy" frame shows 11.7%, but that catches every `.copy()` in
  the framework, not store copies — misleading.) It can't pay off here because (1) stores track few
  locals/fields, so `new HashMap<>(other)` is already cheap, and (2) most copies are immediately
  mutated — the fixpoint copies precisely to refine the then/else/transfer stores — so
  `ensureUnshared()` fires anyway and only adds a per-mutation branch. The surface is also large and
  failure-silent: five maps with ~22 put/remove plus ~10 iterator-`remove()` sites to guard (plus a
  leaky `getFieldValues()`); a single missed guard is silent wrong dataflow everywhere (the
  `AnnotationMirrorSet#addAll` class of bug). Real dataflow allocation lives elsewhere (`[B` UTF-8
  Name decode ~28%, `Object[]` ~13%, `AnnotationMirrorSet` ~2%), none of which COW touches. The
  only sanctioned rescue in this basin is a maintained incremental content-hash on `CFAbstractStore`
  (see the Short list and the rejected equal-store merge short-circuit above), memory-A/B-gated.

- **`String`→`Name` annotation-name migration: replace `AnnotationUtils.annotationName` (String)
  with `annotationNameAsName` (javac `Name`) + `==` identity comparison, framework-wide (June
  2026).** Branch converting the name-dispatch sites in `ValueAnnotatedTypeFactory`,
  `ValueQualifierHierarchy`, `ValueTransfer`, the Index `UpperBound*` checkers, `Units`,
  `BaseTypeVisitor`/`BaseTypeValidator` (`qualAllowedLocations`), `DependentTypesHelper`
  (`annoToElements`), and `AnnotatedTypeFactory` (`aliases`, `declAliases`, `isSupportedQualifier`)
  from value-based `String` comparison to `Name`-identity comparison, backing maps switched to
  `IdentityHashMap<Name,…>`. Premise: "avoid the `Name.toString()` decode + `String.intern()` that
  `annotationName` performs."

  **Measured flat — alloc and wall — even after completing the half-finished form.** As proposed the
  branch slightly *regresses*, because `annotationNameAsName` had no
  `CheckerFrameworkAnnotationMirror` (CFAM) fast path (it pointer-chases
  `getAnnotationType().asElement().getQualifiedName()` where `annotationName` reads a cached field)
  and `isSupportedQualifier(Name)` still called `name.toString()` on every cache miss. To measure the
  *approach* and not the artifacts, the branch was completed: cache a `Name` field on CFAM next to its
  existing interned-`String` `annotationName`; give `annotationNameAsName` a CFAM fast path; replace
  `isSupportedQualifier`'s `getSupportedTypeQualifierNames().contains(name.toString())` with a
  one-time identity-backed companion `Set<Name>` membership test. A/B of that fully-realized version
  vs. master (deterministic `jdk.ThreadAllocationStatistics`, single forked `javac`):

  | corpus | metric | master | realized | delta |
  | --- | --- | --- | --- | --- |
  | nullness, `inherit` shape (~7.4k LoC) | alloc (median of 5) | 4488.0 MB | 4492.3 MB | +0.10% (noise) |
  | nullness, `repeat` shape (~7.3k LoC) | alloc | 785.7 MB | 787.5 MB | +0.23% (noise) |
  | Value, 8 test inputs batched | alloc | 312.6 MB | 312.9 MB | +0.10% (noise) |
  | nullness, `inherit` | wall (2nd-best of 4) | 7.98 s | 8.18 s | noise |
  | nullness, `repeat` | wall | 5.10 s | 5.05 s | noise |

  **Root cause — the targeted allocation does not happen on hot paths.** CFAM, the representation the
  framework manipulates for the overwhelming majority of annotations, already caches its name as an
  `@Interned String` computed once in its constructor, so `annotationName(am)` was *already a field
  read with zero allocation* for CFAM. The `toString().intern()` cost applies only to raw
  `Attribute.Compound` source mirrors, and even there `getAnnotationType().asElement()` is two field
  reads (`anno.type.tsym`). You cannot remove garbage that is not being produced.

  **Ceiling proof.** A deliberately annotation-saturated, checker-bound workload (400 methods × 40
  explicitly-`@Nullable`/`@NonNull`/`@MonotonicNonNull` locals each), profiled at 2596
  `ExecutionSample`s, shows the name-handling frames — `annotationName`, `annotationNameAsName`,
  `areSameByName`, `isSupportedQualifier`, `Name.toString`, `String.intern` — **entirely below the
  sample floor (0 samples).** The hot leaf there is `IdentityHashMap.get` (7.8%), attributed
  (`jfr-analyze self`) ~40% to `ElementQualifierHierarchy.getQualifierKind` — annotation→`QualifierKind`
  resolution, *not* name handling — and caching *that* is itself already measured-and-rejected (see
  "`AnnotationMirror → QualifierKind` second-level cache" above). **General lesson: annotation-name
  comparison/dispatch is not a hotspot; CFAM already caches the decoded interned name, so any
  String→`Name` rerepresentation is flat. This is the dispatch-level analogue of the `isStringEqual`
  false premise (see "Element and name caching"). The annotation-name allocation that *does* show up
  (`[B` UTF-8 decode, ~28% of dataflow allocation) is `Name.toString()` deep in dataflow/store
  machinery, not in qualifier-name dispatch — optimize there, not here.** The branch also seeded three
  anti-patterns worth recording: a `static IdentityHashMap<Name,…>` (Names are interned *per
  compilation context*, so a static identity-map both leaks across compilation tasks and never hits
  cross-context — keep such caches instance-scoped), unused cached-`Name` fields hidden behind a
  class-wide `@SuppressWarnings("UnusedVariable")`, and `Name`-identity assumptions spread across the
  public `javacutil` surface for no measured gain.

---

## Short list

**Verdict (June 2026): the high-value frontier is exhausted — there is no known idea
that is both safe and worth a meaningful fraction of a realistic build.** The leaf
self-time profile is flat (no CF leaf above ~3%), the architectural `getAnnotatedType`
redundancy family is closed (all sub-directions measured: rejected, or already shipped),
and Java-8 inference shipped its big win (PR #1829). Everything still open is one of:
sub-1% and audit-heavy (`qualifiedNameCache`), correctness-blocked (the #602 conditional
cache, the lazy JDK-stub cascade, the `CFAbstractStore` content hash), low-value or
fragile (the two typeinference8 *resolution* items), worst-case-only (the `cond`/`inherit`
super-linears — pathological depth, shallow in real code), or research-scale
(parallelism, the blocked immutability allocation win). Treat new hypotheses here as
"measure the addressable fraction first"; most prior ones died exactly there.

Index (entries are interleaved below; each is tagged with its status inline):
- **Open, low-value:** `qualifiedNameCache` backing map; typeinference8 resolution #3
  (`getInstantiatedVariables`) and #4 (`getSmallestDependencySet`); the `cond` post-dataflow
  conditional cache and `inherit` asSuper depth (size-sweep); `getAnnotatedType` #6 (parallelism).
- **Open but correctness/cost-blocked:** `CFAbstractStore` content hash; lazy JDK-stub cascade; the
  immutability allocation win — **copy-on-write PROTOTYPED and characterized** (branch
  `cow-prototype`, an un-merged experiment — a PR was opened but will not be merged): solves the
  soundness blocker (Guava-validated), allocation −4.8%, but **no
  configuration is wall-positive** (all-caches +5.3%, elementTypeCache-only +1.8%/noise) — a memory/GC
  win only, not a wall win. See the narrative's "Copy-on-write ATMs — PROTOTYPED" entry.
- **Closed, do not re-propose:** PR #1829 incorporation worklist (shipped); `getAnnotatedType` #1
  (per-analysis gvff memo), #2 (pre-flow split cache — built & rejected: unsound across
  override-checkers + flat), #3 (already implemented), #4 (applied-defaults), #5
  (`methodFromUse`/`constructorFromUse`); typeinference8 resolution #1 (dependency graph) and #2
  (`saveBounds`); `AbstractAnalysis.getValue` subnode test (see Tried and rejected).

Capture format: hot method, hypothesis, blockers.

- **Size-sweep super-linearity audit (June 2026) — two unfixed super-linear costs, both
  low-realistic-priority.** Generated eight single-dimension shapes (`/tmp/gen-shapes.py`: `control`,
  `cond`, `chain`, `inherit`, `switchc`, `repeat`, `tryfin`, `loops`), each R independent constructs
  of size D, and swept D, reading deterministic allocation (`alloc-total.java`) and the *marginal*
  Δalloc/ΔD (constant marginal = linear; doubling-per-D-doubling = quadratic). The `control` shape
  confirmed the harness reads linear as linear (flat ~11.6 MB/unit). Results:
  - **`cond` (nested ternary `b?x:(b?x:…)`) — severe super-linear (quadratic→cubic).** Allocation
    635 MB → 1.6 GB → 5.9 GB → **28 GB** at D = 20/40/80/160. Inclusive profile: `getAnnotatedType`
    89% → `TypeFromExpressionVisitor.visitConditionalExpression` 88% → `addComputedTypeAnnotations`
    79%; the allocation driver is the **Issue #602 conditional non-caching** (conditionals are
    excluded from `fromExpressionTreeCache`, so a depth-D nested ternary is rebuilt O(D) per query ×
    O(D) queries = O(D²); same flow-dependence wall as Short-list `getAnnotatedType` #3). A secondary
    *CPU* cost is `TernaryExpressionNode.equals` at 32% inclusive via `AbstractAnalysis.getValue`'s
    structural `contains` (see *Tried and rejected*: the identity fix for that regresses allocation).
    **Possibly-fixable angle (untested):** #602 forbids caching conditionals *during dataflow*, but
    the *post-dataflow* visitor pass has a final, stable store where a conditional-type cache could be
    sound. Sizing = measure conditional-query provenance (dataflow iterations vs. post-analysis
    visitor). Realistic conditionals are shallow, so the everyday win is small; this is mostly
    worst-case (generated-code) protection.
  - **`inherit` (depth-D class chain + D up-assignments) — quadratic.** 320 MB → 4.6 GB at D =
    20→160; `AsSuperVisitor.visitDeclared_Declared` 22% + repeated `getDirectSupertypes` /
    `AnnotatedDeclaredType.directSupertypes` 17% — the asSuper/supertype walk is super-linear in
    inheritance depth (the `directSupertypes` cache does not flatten it). Real hierarchies are rarely
    deeper than ~30, so low practical priority; inherent traversal, no obvious cheap fix.
  - **Linear (no action):** `repeat` (D calls to the *same* method element) is **flat** (~4.2
    MB/unit) — `methodAsMemberOfCache` already makes repeated same-element calls linear, confirming a
    `methodFromUse` element-keyed cache buys nothing (cross-refs #5). `tryfin` (nested try/finally)
    and `loops` (nested for) are flat — no finally-duplication or fixpoint-nesting blowup. `chain`
    (`.self()` chain) and `switchc` (D-case switch) are only mildly super-linear (minor).

- **typeinference8 incorporation worklist + constraint gating — SHIPPED in PR #1829** (closed; was
  deferred from #1805 at ~3%). −8.6%/−11.4%/−19.0% at nesting depth 8/12/20, neutral on realistic
  builds. Full record: Applied optimizations → PR #1829.
- **typeinference8 *resolution* phase: recompute and over-save (the non-incorporation costs).** On a
  moderate-depth heavy workload (`gen-sized-program.py --shape deep-nesting`, depth-8 × 800 methods),
  inclusive time splits roughly `incorporateToFixedPoint` ~47% and `Resolution.resolve` ~34% — but
  most of resolution's time is *re-incorporation* (resolution adds an instantiation bound and
  re-incorporates), so the incorporation worklist above also speeds resolution up. The genuinely
  *separate* resolution costs, in priority order:
  1. **Uncached dependency graph + transitive closure — MEASURED AND REJECTED (June 2026).**
     `BoundSet.getDependencies()` rebuilds the whole variable-dependency graph and recomputes its
     transitive closure (≈O(V³)) from scratch on *every* `Resolution.resolve` call, caching nothing;
     it is ~7.7% of inference time *on the deep-nesting workload*, which suggested caching with
     bound-change invalidation. Instrumented it to compare each recompute's graph (an
     order-independent signature) to the previous one on the same BoundSet. Result: **the redundancy
     and the cost are perfectly anti-correlated.** On deep-nesting (where it is 7.7%) **0%** of
     recomputes are redundant — each resolution step changes the graph (a variable instantiates and
     drops out), so a cache would invalidate every time. On real code (all-systems) **46.6%** of
     recomputes are redundant — but there `getDependencies` is *negligible* (it does not appear in
     the inclusive or self-time profile at all; the inference problems are tiny). So caching saves
     essentially nothing on any workload: useless where it is expensive, free-but-cheap where it is
     redundant. Do not pursue.
  2. **`saveBounds()` snapshots *all* variables every resolution round — MEASURED AND REJECTED
     (June 2026, post-#1805).** `Resolution.resolveSmallestSet` does `new BoundSet(...)` +
     `saveBounds()` (each `VariableBounds.save` allocates 2 `EnumMap`s + 6 `LinkedHashSet` copies, for
     *every* variable in the bound set) as rollback insurance, then discards it whenever
     `resolveWithoutCapture` succeeds — the common case. The standing hypothesis was that this ~3.2%
     could be cut by saving only the mutated subset (medium risk: must save a superset of what is
     mutated). **Re-traced against the current baseline and the ~3.2% no longer reproduces:**
     `saveBounds`/`VariableBounds.save` is **0% self-time** on maximally inference-heavy
     deep-nesting workloads at *both* depth-8 (0/3,368 samples — the original condition) and depth-20
     (1/28,193). The ~3.2% figure predated PR #1805; its `allBoundsProper` skip + work budget moved
     the cost entirely into incorporation (`incorporateToFixedPoint` 47–71%,
     `applyInstantiationsToBounds` 23%), and `resolveSmallestSet`'s inclusive time is now fully
     accounted for by `resolveWithoutCapture`'s *re-incorporation*, not the snapshot. A lazy/COW save
     would chase a ~0% cost. (CPU measured via `settings=profile`; allocation not directly TLAB-traced,
     but overall GC is ~1.7% on this workload, bounding the snapshot's allocation small.) Do not pursue
     without new evidence on a workload where the snapshot itself is shown to be a hotspot. Lesson
     (again): re-trace before acting on a logged percentage — #1805 overtook this one.
  3. **`getInstantiatedVariables()` recomputed every resolution round (O(V²)).** The `resolve` loop
     rebuilds the resolved-variable set (full scan + fresh `LinkedHashSet`) each round. Incremental
     maintenance is possible but complicated by backtracking (`restore()` un-instantiates variables,
     so the set can shrink). Low value alone.
  4. **`getSmallestDependencySet` is O(V²–V³) and mutates the shared dependency sets.** Each round it
     does `dependencies.get(alpha).removeAll(resolvedSet)` for every unresolved variable — re-removing
     already-removed elements across rounds — and mutates the cached dependency map in place. Fragile;
     entangled with #1 (fix together).
- **Architectural: redundant `getAnnotatedType` (the biggest realistic-build lever).** After the
  leaf-level wins were exhausted (the realistic `checknullness` self-time profile is flat — no leaf
  above ~3.4%, and the hot leaves are already-cached lookups called frequently: `getQualifierKind`,
  `getDeclAnnotations`, `isSupportedQualifier`), the remaining cost is *architectural*.
  `getAnnotatedType` is ~47% inclusive, and instrumentation (June 2026) showed it is called **~10×
  per distinct tree** (all-systems redundancy 10.8×, one tree recomputed 987×; loop-heavy artificial
  8.3×). Today only class/method trees are cached (`classAndMethodTreeCache`); expressions recompute
  `fromExpression` + `addComputedTypeAnnotations` every call. That non-caching is *intentional* —
  an expression's type can depend on context (assignment context, capture, the in-progress flow
  store) — but "can depend" ≠ "always differs", and 10× says most recomputations return the same
  thing. Directions, best risk/reward first:
  1. **Per-analysis `getValueFromFactory` memo — MEASURED AND REJECTED (June 2026).** Hypothesis:
     `CFAbstractTransfer.getValueFromFactory` (~19% inclusive) re-queries the same nodes every
     CFG-fixpoint iteration with a flow-stable factory value, so a per-analysis per-node memo would
     remove the redundancy. Instrumented `getAnnotatedType` (a flag set around the body of
     `getValueFromFactory`, plus a per-analysis tree→type map cleared at each
     `CFAbstractAnalysis.performAnalysis`) and measured on real (all-systems 120 and 269 files) and
     artificial (straight-line, nested-loop) workloads. Two findings killed it:
     (a) **The cacheable redundancy is small.** Per-*analysis* dataflow redundancy is only ~2.0× on
     real code and 1.2–1.4× on artificial — *not* the ~10× headline, which conflated cross-analysis
     and visitor calls (a tree queried in 10 different methods is not per-analysis-cacheable). The
     gvff path is ~43% of all `getAnnotatedType` calls and ~43% of those are per-analysis repeats, so
     the memo's theoretical ceiling is ~18% of `getAnnotatedType` calls — before soundness.
     (b) **The factory value is not stable within one analysis, so the memo would be unsound.** Of
     the repeated gvff queries, ~8% return a *different* type on real code and ~18% in loop-heavy
     code, because `getAnnotatedType` consults the evolving flow store for subexpressions (the type
     of `a.b` depends on the refined type of `a`, which changes across fixpoint iterations) — exactly
     the soundness crux flagged here. And the instability is *highest where the redundancy is
     highest* (loops). A per-analysis memo would cache stale/wrong types; a per-*iteration* memo would
     be sound but the within-iteration redundancy is ~0. Low value and unsound — do not pursue.
  2. **Pre-flow expression-type ("split") cache — BUILT AND REJECTED (June 2026).** Hypothesis:
     `addComputedTypeAnnotations(Tree, ATM)` splits at the `if (this.useFlow)` boundary into a prefix
     2a (`applyQualifierParameterDefaults` → tree/type annotators → `defaults.annotate`) and a flow
     suffix 2b (`getInferredValueFor` + `applyInferredAnnotations`); since flow lives only in 2b, cache
     the *pre-flow* (post-2a) type per tree and recompute only 2b. Per-factory, per-hierarchy
     measurement shows the pre-flow type *is* stable, so the cache looked sound:

     | category | all-systems (120f) | loops |
     | --- | --- | --- |
     | value leaf (literal / var / field / param id) | disagree 0, compl 0 / 58,447 repeats | 0 / 0 / 130,894 |
     | compound expression | disagree 213 (0.69%), compl 0 / 30,949 | 0 / 0 / 33,604 |
     | type-name identifier (class/enum) | excluded (≈0.7% context-dependent) | — |

     (Value leaves never disagree or under-annotate; the compound ~0.69% residual is the genuinely
     context-dependent `NewClass`/`NewArray`/conditional set — the #602 trees of #3 below; type-name
     identifiers ~0.7%, excludable by element kind.) A working cache was then implemented — a
     per-factory `IdentityHashMap<Tree, AnnotatedTypeMirror>` of frozen pre-flow types populated just
     before the `useFlow` block, with `getAnnotatedType` overridden to return `cached.deepCopy()` +
     re-applied 2b on a value-leaf hit, cleared per CU, toggled by `-Dcf.preflowcache`. **Two
     independent findings killed it:**
     - **Unsound for any checker that overrides `addComputedTypeAnnotations`.** `IndexTest` fails
       (`PredecrementTest.java:8`, a spurious `array.access.unsafe.low`). `UpperBoundAnnotatedTypeFactory`
       (and Lower Bound, Optional, Lock, Signedness) override `addComputedTypeAnnotations` to add
       flow-dependent annotations **after** `super` — Index pulls the Value Checker's type and calls
       `addUpperBoundTypeFromValueType` (its comment cites `"int i = 1; --i;"`, exactly the failing
       test). Intercepting at `getAnnotatedType` and re-applying only the GATF-level flow step bypasses
       that subclass tail, so hits drop the index refinement. Caching *inside*
       `addComputedTypeAnnotations` so the subclass tail still runs would need a deep "replace
       annotations onto the passed-in type" and saves only 2a (not `fromExpression`) — more complexity,
       smaller win. A nullness-only diagnostic diff (cache on vs off) was clean, which is exactly why
       nullness-only validation missed it.
     - **Flat-to-mixed even where it is sound (nullness).** Deterministic A/B (median-of-5 alloc;
       2nd-best-of-4 wall), off vs on: all-systems alloc 2658.7 → 2616.1 MB (−1.6%), wall 12.12 →
       12.10 s (~0); loops alloc 1078.2 → **1100.1 MB (+2.0%, worse)**, wall 7.18 → 6.84 s (−4.7%). The
       projected ~4–15% (raw 2a self-time) did not materialize — the per-hit `deepCopy` + `freeze` +
       map overhead roughly cancels the saved 2a, and the realistic corpus is noise.

     **Do not pursue.** The companion "phase-scoped full cache" idea would hit the same
     compose-with-overrides wall. **Lessons:**
     - *Stability ≠ cacheable.* Per-hierarchy stability proved the cache could be sound in isolation,
       but a framework-level `getAnnotatedType` cache must **compose with subclass
       `addComputedTypeAnnotations` overrides**, and even then the `deepCopy` cost makes it a wash.
       Validate such a change on the override-checkers (Index/Lock/Optional/Signedness), not just
       nullness.
     - *Measuring per-tree type stability has two traps* (both nearly produced a false "unstable
       25–59%, unsound" verdict): **(1)** never compare via `AnnotatedTypeMirror.toString()` — it
       conflates *cross-hierarchy completeness* (a literal printing as `int` vs `@Initialized int` is
       the Initialization annotation absent-vs-present, not a within-hierarchy change) with real
       disagreement; compare per hierarchy as {top-name → annotation}. **(2)** a checker runs *several*
       `AnnotatedTypeFactory` instances (a `nullness` compile drives `NullnessNoInit`, `Initialization`,
       `InitializationFieldAccess`, `KeyFor`) — never key instrumentation or a cache on `Tree` in
       `static` state, or you compare one type system's snapshot against another's. (Note:
       `getTopAnnotations()` is *not* an ordering hazard — it returns a build-once `ArrayList`-backed
       `AnnotationMirrorSet` over a `TreeMap<QualifierKind,…>`, a stable deterministic order.)
  3. **Split flow-independent structure from flow-dependent annotations — MEASURED, ALREADY
     IMPLEMENTED (June 2026).** The hypothesis was: `fromExpression` (~24% inclusive) builds a
     deterministic-per-tree skeleton, so cache the frozen skeleton and re-apply only the
     annotation/default layer on a copy. **This is already what `AnnotatedTypeFactory.fromExpression`
     does:** `fromExpressionTreeCache` stores the frozen `TypeFromTree.fromExpression` skeleton per
     tree, returns `cached.deepCopy()` on a hit, and the caller (`getAnnotatedType`) re-applies only
     `addComputedTypeAnnotations` on that copy. Instrumented the build-vs-copy split (counters around
     the hit-copy, the miss build `TypeFromTree.fromExpression`, and the put `frozenDeepCopy`; single
     forked `javac`). The split criterion ("pays off only if build ≫ copy") is *satisfied and already
     harvested*: build is 96–98% of measured `fromExpression` time, copy only 1.5–4%, and the cache
     already serves 66% (synthetic generic size sweep) to 79% (all-systems ×80) of calls as ~0.4 µs
     `deepCopy`s. The residual 98% "build" is the **irreducible first construction of each distinct
     cacheable tree** — not cacheable away. The only remaining redundancy is the Issue #602 exclusions
     (`NewClassTree`/`NewArrayTree`/`ConditionalExpressionTree`): on all-systems, ~20% of build time
     (an upper bound — build timing is inclusive of nested re-entrant `fromExpression` calls) is spent
     rebuilding just 153 distinct trees ~16× each (94% repeats). That is **blocked by #602**: those
     trees' types are flow-dependent (they change across fixpoint iterations), the same soundness crux
     that rejected the per-analysis `getValueFromFactory` memo (#1, where 8–18% of flow-dependent
     repeats return a *different* type). No safe caching win remains. Do not pursue.
  4. **Cache the *applied* defaults** (not just the `DefaultSet`, which is already cached) per
     (element, structural shape) to skip the per-call `DefaultApplierElementImpl.scan` (~12% incl).
     **MEASURED AND REJECTED (June 2026)** — prototype built and A/B'd; see *Per-CU tree-defaults
     memoization* under "Tried and rejected". High hit rate (80–86%) but net neutral-to-worse on
     every axis, because the per-call defaulting scans are cheap in-place annotation mutations while
     the cache key/snapshot machinery is not.
  5. **`methodFromUse`/`constructorFromUse` per-tree memo** (~15% incl), scoped like #1.
     **DONE / NOT OPEN — now MEASURED (June 2026), not just argued by analogy.** The flow-independent
     `(methodElt, receiverType)` form already shipped as the `methodAsMemberOfCache` (PR #1777 — see
     "`methodFromUse`/`asMemberOf` cache — APPLIED" below); the additional *per-tree* form hits
     direction #1's soundness wall (the result depends on the evolving flow store). Instrumented both
     protected impls (`record` at each return: per-tree redundancy via an `IdentityHashMap`, and the
     *unsound fraction* = repeats whose `executableType.toString()+typeArgs` differs from the prior
     result for the same tree; plus a simulated `executableType.deepCopy()` for copy cost; single
     forked `javac`, `inferTypeArgs` path only). Findings: redundancy is high and copy is cheap (the
     tempting part) — `methodFromUse` ~8.0× redundancy on both the size sweep and all-systems, copy
     0.5–1.6% of build; `constructorFromUse` ~23× on all-systems, copy ~1.4% — **but the unsound
     fraction is decisive: 43–45% of `methodFromUse` repeats and ~15% of `constructorFromUse` repeats
     return a *different* result** (receiver/argument types are flow-refined and evolve across fixpoint
     iterations). That is worse than the per-analysis memo (#1, 8–18%) that was already rejected, so a
     persistent per-tree memo would serve a stale/wrong type ~45% of the time it hit. The cacheable
     (flow-independent) part is already the `methodAsMemberOfCache`; the expensive part is exactly the
     flow-dependent viewpoint-adaptation/inference, which is the part that varies — same structure as
     #3's residual. Closed by measurement. Do not re-open as "unshipped."
  6. **Parallelize checking across classes/methods** — the only constant-factor-by-core-count lever,
     but the factory + its caches + javac symbol state are shared mutable state; research-scale.
  Not pursued this session: the immutability program (delete `deepCopy`, ~10% inclusive) is the other
  big architectural bet and is covered in its own narrative below; the notes say it is already largely
  harvested.
- **`ElementUtils.qualifiedNameCache` backing map.** Hot method
  (`getQualifiedName` underlies `annotationName`, `getBinaryName`, the `isX`
  type predicates, etc.). Today it is a
  `Collections.synchronizedMap(new WeakHashMap<>())`, and an inline TODO already
  asks whether an `IdentityHashMap` would be better. Two separable costs on the
  hot path: (1) the `synchronizedMap` lock on every `get`/`put`, and (2)
  `WeakHashMap`'s reference-queue expunging plus a `WeakReference` allocation per
  `put`. javac `Symbol`s use identity `equals`/`hashCode`, so the key semantics
  would not change. Open questions, none answerable without measurement: the lock
  cannot be dropped without auditing every reachable thread, and a static cache is
  more exposed than the per-factory field the campaign already de-synchronized;
  the language server and the Gradle daemon run analyses in long-lived JVMs, where
  weak keys let old compilations' `Symbol`s be collected — a strong
  `IdentityHashMap` would retain them. **JFR capture (June 2026,
  `allNullnessTests -PmaxParallelForks=1`)** confirmed the lock/expunge cost:
  `WeakHashMap.get` via `Collections$SynchronizedMap.get` appeared at 110/18,969
  execution samples (0.58%), with callers split across `annotationName`,
  `isSupportedQualifier`, `AnnotationFileElementTypes`, and
  `normalizeAndCheck`. Still needs the thread-reachability audit and daemon/LSP
  memory analysis before any change.
- **Maintained content hash on `CFAbstractStore` to make same-size `equals` O(1).** The only way to
  rescue the rejected equal-store merge short-circuit (see Tried and rejected): the residual cost is
  the same-size/different-value `supersetOf` walk, which a running content hash — updated incrementally
  on every `put`/`remove`/`clearValue`/`insertValue` and compared before the walk — could reject in
  O(1). Blockers, none cheap: (1) it replaces the deliberately-cheap size-only `hashCode` with one that
  must stay consistent across *all* store mutation sites (the exact mutable-cache-invariant hazard
  CLAUDE.md flags for `AnnotatedTypeMirror`); (2) on the loop-fixpoint case the store changes every
  iteration, so the hash is recomputed/invalidated each time and the saving may not materialize; (3)
  the ceiling is the ~1.4% allocation the short-circuit was already worth — no CPU win. Only worth
  prototyping if a memory-bound `checknullness` A/B (default heap, warm daemon) first shows that
  allocation delta converting to wall clock. Spotted June 2026 (PR #1793 review) while auditing why
  `==`/`.equals` could not make the store short-circuit pay off.

A June 2026 inclusive-time / co-occurrence investigation (looking for
*architectural* redundancy rather than leaf hot spots, since the leaf profile is
now flat) surfaced a further candidate, blocked by a correctness invariant, which
is why the campaign left it:

- **The lazy JDK-stub cascade runs the full type-annotation pipeline during
  parsing, uncached.** Stacks captured on `allNullnessTests` show
  `maybeParseEnclosingJdkClass` → `annotateSupertypes` → `directSupertypes` →
  `addComputedTypeAnnotations` → `DefaultQualifierForUseTypeAnnotator` →
  `getExplicitAnnos` → `fromElement` → `maybeParseEnclosingJdkClass` … repeating 3–4
  times in one stack: computing the defaults/supertypes of one JDK class pulls in
  another class's stub, whose own defaults/supertypes are then computed, all while
  `stubTypes.isParsing()` disables the factory caches, so the same work is redone
  during real checking. The static `StubUnit` cache above removes the *parse* half
  of this; the *resolution/defaulting* half remains. **Blocker:** the
  caching-disabled-during-parsing rule exists because partially-loaded stubs yield
  incomplete annotations; changing when defaults are computed for JDK supertypes is
  correctness-sensitive and needs its own design.

### Realistic-workload venues (June 2026 `checkNullness` investigation)

Context for future sessions: the leaf self-time profile is flat (no single CF leaf
above ~3.6%), so the campaign's per-leaf wins are exhausted. The remaining cost is
*architectural* — the per-node type-computation pipeline. Use inclusive-time and
allocation analysis, not leaf self-time, to make progress.

**Pick the right workload.** `allNullnessTests` is dominated by test-harness
amplification — it runs hundreds of tiny per-directory compilations in one worker
JVM, so JDK-stub work (parse + resolve) is ~28–32% inclusive there but only ~6% in a
real single compilation. For *realistic* venues, profile a single forked-javac
compile: `:checker:checkNullness` (then isolate the worker `cknull-<pid>.jfr` — the
file whose stacks contain `GenericAnnotatedTypeFactory.performFlowAnalysis`; the
launcher/daemon/shadowJar files are noise). In that worker: flow analysis ≈ 38%
inclusive, `getAnnotatedType` ≈ 47%, and — crucially — `Object[]` is ~61% of all TLAB
events, ~91% of which are `IdentityHashMap` backing arrays from `AnnotatedTypeScanner`
(`reset` 52%, `<init>` 17%) and `AnnotatedTypeCopier.visit` (22%). Flow analysis's own
self-time *is* the type pipeline (scanning, defaulting, copying, map lookups,
`TreePath`, symbol completion), not dataflow logic — the dataflow framework itself
(`CFStore`/`CFValue`) does not appear in self-time.

**Where the wall-clock goes** (from `jfr-analyze.java phase` on the worker, June 2026,
post-scanner-reuse). The compile is **CPU-bound**: ~96% on-CPU Java, GC pauses only
~1.35 s (~4%, `sumOfPauses`), and real I/O ≈ 0 (the many `NativeMethodSample`s are
99.5% `EPoll.wait` on the idle Gradle messaging thread — exclude them). The on-CPU Java
time splits, mutually exclusively by innermost subsystem (so the type computation that
dataflow and the visitor *trigger* is attributed to the type factory, not to them):
- **Annotated-type computation ≈ 54%** — `getAnnotatedType`/`fromElement`, defaulting,
  supertypes, ATM copying/scanning, plus its `javacutil` support (`ElementUtils`,
  `AnnotationUtils`, qualifier-hierarchy lookups, which make up most of the separate
  "Other CF" 14% bucket). This is the core cost and where the campaign focused.
- **javac internals ≈ 32%** — but **~77% of that is CF-triggered** (forced
  `Symbol.complete`/`apiComplete`, `Name`/UTF-8 decoding via `Convert.utf2chars`,
  `TreePath` construction, tree walks). Only ~7% of the *total* is javac's autonomous
  front-end (parse/enter/attribute). So ~25% of all time is **CF reaching into javac**.
- **Dataflow machinery ≈ 5%** (CFG build + fixpoint + transfer/store, excluding the type
  lookups it calls — note this is the *exclusive* figure; flow analysis is ~38%
  *inclusive* precisely because it triggers so much type computation).
- **Stub/JDK annotation loading ≈ 3%** (small in one compile; the ~28% monster only in
  the test suite). **Visitor check logic itself ≈ 1%** — almost all cost is *producing*
  the annotated types, not checking them.

Two takeaways for picking venues: (1) GC/allocation is not the wall-clock bottleneck on
a single compile (which is why the scanner-reuse and `AnnotationMirrorSet` allocation
wins did not move single-compile time — their value is GC pressure at scale); CPU is.
(2) The largest non-obvious CPU slice is CF driving javac (symbol completion + name
decoding + tree/path walks), bigger than dataflow + stubs + visitor combined.

**Guava cross-check (first hotspot JFR of a large heavy-generics codebase, June 2026).**
`test-guava-nullness.sh` (Nullness Checker on the `guava` module, 625 files; JFR injected
via the forked-compiler `-J` args in the `checkerframework-local` profile; 7,814
ExecutionSamples). Two findings:
- **The leaf self-time profile generalizes — nothing new at the leaf.** Same flat shape as
  `checkNullness`: no CF leaf above **3.85%**; the top is `IdentityHashMap.get`/`put` (≈6.8%
  combined), the ATM traversal (`AnnotatedTypeScanner.scan`/`visitDeclared`/`reduce` ≈7%),
  `Symbol.apiComplete`, `DefaultApplierElementImpl.scan`, `AnnotatedTypeCopier.visitDeclared`,
  `AnnotatedTypeMirror.createType`. Allocation: `[Ljava.lang.Object;` 32%, `ArrayList` 8.7%,
  `IdentityHashMap` 5.5%, `AnnotationMirrorSet` 4.5% — the same ATM-pipeline allocation.
- **New *fact* (not a new lever): type-argument inference is a top-2 inclusive cost on heavy
  generics, where it is negligible on `all-systems`/`checkNullness`.** `getAnnotatedType` 50%
  inclusive (as usual), but then `methodFromUse` **24.6%** → `inferTypeArgs` **23.0%** →
  `InvocationTypeInference.infer` 22.4% → `ConstraintSet.reduceOneStep` 14.1%. Decomposing the
  23% inference slice (`cooccur`): **50% of it is under `getAnnotatedType`** (building
  argument/receiver/bound types) and **28% under `incorporateToFixedPoint`** (the machinery
  PR #1829 already optimized); the inference-*specific* self-time (`TypeConstraint.hashCode`,
  `ConstraintSet`, constraint-collection churn — `LinkedHashMap`/`ListBuffer`/`List$2` ≈6% of
  allocation) is small and diffuse. So Guava **reinforces** rather than overturns the Short
  list: the biggest lever is still `getAnnotatedType` (now shown to dominate inference too, not
  just checking), inference's big win already shipped (#1829), and the resolution items (#3/#4)
  stay low-value — their self-time is tiny even here. Takeaway for future sessions: `all-systems`
  and `checkNullness` **under-represent inference**; profile **Guava (or jspecify-conformance)**
  for any inference work, but expect the cost to be the type pipeline it drives, not a clean
  inference-specific frame.

Open venues, roughly by tractability:

- **Reduce ATM deep copying (`AnnotatedTypeCopier.visit` = 22% of `Object[]`).**
  Defensive deep copies exist only because ATMs are mutable (every `fromElement`
  cache hit, many `getAnnotatedType` paths). `ATF.getElementAnnotations` (committed)
  was a one-caller nibble. The real lever is **copy-on-write annotation sets or
  immutable ATMs**; this also unblocks the `directSupertypes` cache above. Large,
  architectural, high value. **This is now an active staged program — see
  "AnnotatedTypeMirror value-semantics program" below.**

### AnnotatedTypeMirror value-semantics program + cache campaign (narrative; June 2026)

This subsection is the **detailed methodology log** for the cache campaign and the immutability
program. Canonical statuses live in the top-level sections; this is the "how we got there" record.
**Status map:**
- **Shipped** (see Applied optimizations): the `methodAsMemberOf`, `directSupertypes`, and
  `elementType`/Phase-1 caches (PR #1777); the smaller-scope `declarationFromElement` scan (PR #1780);
  the **`freeze()` mechanism + the `AnnotatedTypeCopier` vararg-aliasing fix + freezing all eight
  cache masters (PR #1798)** — the immutability program's *foundation*, behavior-neutral and
  perf-neutral.
- **Tried and rejected** (see that section): `constructorFromUse` cache, poly-deferral,
  `declarationFromElement` via `trees.getTree`/single-pass-map, shrinking the heavy caches; and
  (PR #1798 session) the **cache-boundary flips** (returning the shared frozen instance instead of a
  copy — Element boundary, `methodFromUse` copy-elision), **`hashCode` caching on frozen ATMs**, and
  the **shallow-location defaulting shortcut**.
- **Open** (see "Open items" at the end of Short list): the immutability *allocation* win (delete
  `deepCopy`/drop copy-on-return) is **blocked** — see the load-bearing-copy finding below — pending
  copy-on-write or eliminating redundant re-annotation (Defaulting Phase 2).

Goal of the immutability program: make ATMs effectively immutable / copy-on-write so
`deepCopy`/`shallowCopy` can be deleted and the cache boundaries stop paying the deep-copy tax
(`AnnotatedTypeCopier` ~2% self-time + the dominant share of `Object[]` allocation) — and the
+50–70 MB the shipped caches retain goes away.

**Status after PR #1798: the foundation shipped; the allocation win is blocked, not merely "next".**
PR #1798 makes a frozen type effectively immutable (a `frozen` bit; `checkMutable()` on the three
primary-annotation sinks `addAnnotation`/`removeAnnotation`/`clearAnnotations`, with
`primaryAnnotations.makeUnmodifiable()` as a backstop; a cycle-safe deep `freeze()` that freezes only
already-initialized components, with the lazy getters freezing components they create later) and
freezes every cache master, so a latent in-place mutation of a cached type now fails fast with
`BugInCF` instead of silently corrupting a shared value. The caches still hand out a `deepCopy()` on
every hit, so it is behavior-neutral and (measured, PR #1798) perf-neutral. **But four independent
attempts this session showed the cache-return `deepCopy` is load-bearing** — the dominant consumers
mutate the result, so removing the copy needs a deeper change than a boundary flip. The evidence and
the dead ends are below; the immutability program is therefore **paused at its foundation**, not the
"recommended next direction" it was before this session.

**Validation spike (DONE, GO) — and this spike SHIPPED: see "`methodFromUse`/`asMemberOf` cache —
APPLIED in PR #1777" below. This paragraph is the precursor, NOT an open candidate.** A throwaway
`methodFromUse` cache (non-generic methods, key `(methodElt, structural receiverType, inferTypeArgs)`,
copy-on-store/return) on `:checker:checkNullness`: **66.7% hit rate**, `AnnotatedTypes.asMemberOf`
(12.4% inclusive) eliminated on hits, net allocation down even with the copy-tax, ~5% fewer on-CPU
samples; the structural-key hashing and copy-tax stayed below the inclusive threshold (cheap). Payoff
confirmed → productionized as the `methodAsMemberOfCache`.

**Standalone caching needs poly-handling + opt-outs — but NOT the full immutability program.**
Two experiments settled this; a methodological trap nearly sent us down the wrong road, so the
correction is recorded carefully.

*The recompute cross-check is INVALID for this computation.* A natural validator — on a cache hit,
recompute `computeMethodTypeAsMemberOf` and `assert` it structurally equals the cached value — fired
across **~20 checkers** (with either identity-based or `Types.isSameType`-based comparison). It looked
like a deep "value-identity wall." **It was an artifact:** an idempotency probe (compute the same
`(tree, methodElt, receiverType)` *twice in a row* and compare) showed the two results have *identical*
`toString()` but compare **unequal** — because substitution / capture conversion mints **fresh
type-variable and captured-type instances on every call** (`isSameType(CAP#1, CAP#2) == false`). So
the recompute cross-check can *never* succeed on any type-variable- or capture-bearing result,
regardless of whether the cache is actually correct. **Do not use a recompute-and-compare cross-check
to validate ATM-producing caches; validate with `alltests` diagnostics instead.** (`EqualityAtmComparer`
also compares underlying types by identity — line 55, `ut1.equals(ut2)`, javac `Type` has no value
`equals`, `@SuppressWarnings("TypeEquals") // TODO` — which contributes, but `isSameType` does not fix
it because of the fresh-capture issue above.)

*The real breakage, validated by diagnostics (cache on, cross-check OFF), is bounded — ~9 suites:*
`NullnessTest` (3, polymorphic qualifiers), `H1H2CheckerTest`/`SubtypingEncryptedTest` (poly),
`ValueTest`/`ValueIgnoreRangeOverflow`/`ValueNonNullStringsConcatenation`/`ValueUncheckedDefaults`
(the Value checker — its method results are call-/argument-dependent), `IndexTest` (MethodVal
reflection), `InitializedFieldsValueTest`. The ~20-checker "breadth" was the cross-check artifact, not
real. So caching the `(methodElt, receiverType)`-determined substitution is sound for most checkers;
it is unsound where the method type is genuinely call-dependent (polymorphic-qualifier resolution;
Value; reflection).

*Decision — bounded, not the megaproject.* The wall-clock-win cache is achievable with:
(1) a **polymorphic-qualifier guard** — skip caching when the method's *declared* type contains a
`@Poly*` qualifier (must check the **declared** type, not the `computeMethodTypeAsMemberOf` result:
`methodFromUsePreSubstitution` — its boolean param is literally `resolvePolyQuals` — resolves the
poly qualifiers to concrete ones before the result, so scanning the result misses them; cached per
element);
(2) a **per-checker opt-out predicate** `shouldCacheMethodAsMemberOf()` (default true) for genuinely
call-dependent checkers — overridden false in `ValueAnnotatedTypeFactory` (results computed from
argument values) and `MethodValAnnotatedTypeFactory` (reflection);
(3) **validate via `alltests` diagnostics**, NOT a recompute cross-check.
The copy-tax for value stability is cheap (measured). This is bounded work, far less than the
immutability rewrite. **The immutability program is therefore decoupled: it remains worthwhile for the
*allocation* win (deleting `deepCopy`, ~22% of `Object[]`) and the clean end-state, but it is NOT a
prerequisite for the wall-clock-win cache.**

**`methodFromUse`/`asMemberOf` cache — APPLIED in PR #1777.** The cache as above is implemented in
`AnnotatedTypeFactory.methodFromUse` (the inner 4-arg overload): cache the
`(methodElt, receiverType)`-determined `computeMethodTypeAsMemberOf` result, keyed with a cache-local
`isSameType`-based structural comparison (`IsSameTypeAtmComparer`, so structurally-equal receivers
share an entry and distinct captures stay distinct, without touching the global `ATM.equals`);
deep-copy on store/return; skip declared-`@Poly*` methods; `Value`/`MethodVal` opt out.
**Correctness:** full `:framework:test` + `:checker:test` pass (0 diagnostic failures); the framework
nullness self-check passes. **Performance — single-subproject slice (`:checker:checkNullness`, two
captures):** `asMemberOf` inclusive 12.4% → absent; on-CPU Java samples 3,443 → ~2,690 (−22% *of that
one worker*); GC pause down too. A cache *hit* skips all of `computeMethodTypeAsMemberOf` (including the
`getAnnotatedType(methodElt)` deep-copy and fake-overrides), which is why the win exceeds `asMemberOf`
alone. **CAVEAT — this −22% is a slice, not the build** (see the full-build A/B below): it is the
on-CPU type-factory work of *one* forked compile, which is a minority of `./gradlew checknullness`
wall-clock (10 subprojects + per-fork JVM startup + parse/enter/attribute). Always state the
combined-cache full-build number, not this slice, as the headline.

*Deferring poly resolution past the cache — DESIGNED + PAYOFF MEASURED, NOT WORTH IT (for realistic
workloads).* The idea: drop the declared-`@Poly*` guard so poly methods are cached too, by running
`methodFromUsePreSubstitution` per-call after the cache instead of inside `computeMethodTypeAsMemberOf`.
The clean design avoids any per-call cost: keep the **cached** per-element `methodDeclaresPolymorphicQualifier`
check and use it to *route* the hook (run it inside the cached compute for non-poly methods — unchanged;
defer it to a per-call copy only for poly methods), rather than to *block* caching. So non-poly methods
are untouched and poly methods would gain a cached `asMemberOf`. **But the payoff is negligible:** an
instrumented full `./gradlew checknullness` found that **only 0.1% of cacheable method calls (322 of
250,000) are on poly-declared methods** — the guard already lets 99.9% of method calls into the cache
(86% hit rate). Caching the remaining 0.1% (even at their 88% would-be hit rate) is ≈ zero wall-clock,
not worth the soundness risk of reordering poly resolution after `asMemberOf`/viewpoint-adapt (and after
MustCall's non-owning→top adjustment, which shares the hook). The design is sound and *would* help a
checker whose calls are dominated by polymorphic methods, but that is not the realistic target. **Keep
the guard.** (Lesson, again: a "drop the guard / extend the proven cache" idea still needs its payoff
measured — the guard turned out to cost 0.1% of coverage, not the meaningful slice assumed.) Mechanism
detail retained below for whoever revisits it:

The poly guard and the type-variable non-guard are not a fundamental asymmetry; they are
an artifact of *where in the `methodFromUse` pipeline* each call-site specialization happens relative to
where the cache stores its value. The cache stores `computeMethodTypeAsMemberOf` (stops after
`asMemberOf`). **Method type arguments are substituted *after* the cache** — `findTypeArguments` +
`typeVarSubstitutor.substitute` run per call on the `deepCopy()` (inner `methodFromUse`, ~lines
2735–2747) — so the cached value is still *generic* in the method's type variables and two calls with the
same `(methodElt, receiverType)` but different (explicit or inferred) type arguments correctly diverge on
their own copies. That is exactly why the key is `(methodElt, receiverType)`, not `(…, typeArgs)`, and
why type variables need **no** guard; guarding them would needlessly disable the cache for every generic
method. **Polymorphic qualifiers, by contrast, are resolved *inside* the cached computation** — at
`methodFromUsePreSubstitution(tree, …, resolvePolyQuals)` (~line 2792), which reads the call-site
arguments and bakes concrete qualifiers in — so the stored value is already specialized to one call
site's arguments, which the key does not capture; hence the guard. **If poly resolution were moved to a
post-cache, per-call step (the same side of the boundary as type-arg substitution), the cached value
would be poly-generic and the declared-`@Poly*` guard could be deleted** — recovering the
Nullness/H1H2/Subtyping suites that currently bypass the cache. Larger and riskier than the guard
(it relocates `methodFromUsePreSubstitution`'s poly handling onto the copy and must preserve the
arguments→qualifiers resolution semantics), so deferred; the guard is the bounded, sound choice for now.
Note the base `methodFromUsePreSubstitution` is empty and its only contract is the `resolvePolyQuals`
parameter, so the declared-`@Poly*` guard covers exactly the tree-dependent work that bakes into the
cached value; an override doing *other* tree-dependent work there must use the `shouldCacheMethodAsMemberOf()`
opt-out instead (which is why Value/MethodVal disable the cache wholesale).

**`directSupertypes` cache — APPLIED in PR #1777.** `directSupertypes(type)` is a
pure function of `type`'s structure and annotations (the only hook, `postDirectSuperTypes(type,
supertypes)`, takes no tree/args; it copies the receiver's effective annotations and applies
element-based defaults), so — unlike `methodFromUse` — it needs **no poly guard and no per-checker
opt-out**. `AnnotatedTypeFactory.getDirectSupertypes(AnnotatedDeclaredType)` caches it, keyed on the
type with the same cache-local `isSameType` structural comparison; deep-copy on store/return (callers
mutate the supertypes' annotations); `AnnotatedDeclaredType.directSupertypes()` delegates to it.
**Correctness:** full `:framework:test` + `:checker:test` pass (0 failures); framework nullness
self-check passes. **Performance (single-subproject slice, `:checker:checkNullness`):**
`directSupertypes` 13.5% inclusive → absent; primarily an **allocation** win — TLAB events −13.5%
(`Object[]` −17.5%) — plus a modest ~5% on-CPU on that slice.

**Full-build A/B — the headline numbers (June 2026).** The slice figures above (`−22%`, `−26%`) badly
*overstated* the build-level impact because they profiled a single forked compile (~2,600 samples /
~26 s), whereas the unqualified `./gradlew checknullness` runs the checker over **10 subprojects**
(checker, checker-qual{,-android}, checker-util, dataflow, docs, framework, framework-perf,
framework-test, javacutil), all routed through **one persistent Gradle compiler-worker JVM**. Profiling
the full task (`--no-daemon`, JFR on every JVM via `JAVA_TOOL_OPTIONS`, then analyzing the one large
worker file) gives a complete trace of ~15.5–17k samples / ~155–172 s — 6× the slice. Clean A/B,
both caches applied vs. reverted (processor `shadowJar` rebuilt each side, identical `--no-daemon` run):

| metric (full `./gradlew checknullness`) | baseline | with caches | delta |
|---|---|---|---|
| on-CPU Java samples (whole worker) | 17,227 | 15,555 | **−9.7%** |
| Type-factory phase samples | 7,121 | 5,893 | **−17.2%** |
| wall clock, `--no-daemon` | 229 s | 209 s | **−8.7%** |
| wall clock, warm daemon (user-observed) | ~180 s | ~157 s | **~−13%** |

So the two caches are worth **~9% (cold) to ~13% (warm-daemon) end-to-end wall clock**, and ~17% of the
type-factory phase specifically — a real, solid win, but roughly half what the single-worker slice
implied. TLAB allocation is down correspondingly. Both caches are decoupled from the immutability
program. **Methodology lesson (do not repeat): profile `./gradlew checknullness` (the full
multi-subproject task), not `:checker:checkNullness` (one subproject), and report the whole-worker
sample delta + wall clock, never a single phase's inclusive % as if it were the build.** The
`record-jfr.sh` "analyze the largest file, the rest is noise" advice is correct *for a single-project
task* but silently undercounts here, because the largest file is the only real worker and it contains
all 10 compiles — analyze it, but know it is the whole build, not one subproject.

**Post-cache re-profile — next venues (June 2026, full-build worker, 15,555 on-CPU samples).**
With both caches applied, the `jfr-analyze.java phase` breakdown on the full `./gradlew checknullness`
worker: **Type factory 37.9%** (baseline 41.3% — the two caches removed ~3.4 points of the total, i.e.
−17% within the phase), **javac internals 34.7%** (now the *relative* leader), Other CF 13.3%,
Dataflow 6.8%, Stub 4.2%, Visitor 3.0%. The leaf self-time profile is very flat — no CF leaf above
~3% (`HashMap.getNode` 3.4%, `QualifierDefaults$DefaultApplierElementImpl.scan` 2.9%,
`AnnotatedTypeScanner.scan` 2.8%) — so the remaining work is squarely architectural / aggregate,
not single-leaf. Re-prioritized venues:

- **CF driving javac internals — RE-MEASURED on current HEAD (Phase 1 + all caches committed), and
  the picture changed substantially.** Fresh full `./gradlew checknullness` trace: 12,182 on-CPU
  samples (down from ~14.7k — the committed work landed), **javac internals now the #1 phase at
  37.2%** (type factory 34%). The breakdown is *not* what the pre-cache bullet below assumed:
  - **`AnnotatedTypeFactory.declarationFromElement` = 13.1% (1,597 samples) — the single largest
    CF→javac cost, but no cheap fix found yet.** It caches (`elementToTreeCache`, cleared per CU) but
    the *miss* path's `default` branch — local variables, parameters, resource/exception params, type
    params — calls `TreeInfo.declarationFor(sym, root)`, which **scans the whole compilation-unit tree**
    to find one declaration (~95% of its self-time is `JCIdent.accept`/`DeclScanner.scan`/`TreeScanner.scan`;
    hit rate only ~9%; the scan branch is ~19.5% of misses but ~all the cost).
    **Tried `trees.getTree(elt)` for the 4 variable kinds — VERIFIED CORRECT, but a NO-OP, reverted.**
    The hope: `trees.getTree` is the position-based path class/method/field already use and which does
    not show up in the `DeclScanner` cost. Correctness checked exhaustively (instrumented: 8,124/8,124
    match vs the scan, 0 differ, 0 missed; `:framework`/`:javacutil`/`:dataflow`/`:checker` tests all
    pass) — but the warm-daemon A/B was flat (total on-CPU 12,182→12,140, `declarationFromElement`
    1,597→1,517) because **`trees.getTree` for a local/parameter internally calls the same
    `TreeInfo$DeclScanner`** — it relocates the scan, it doesn't avoid it (post-change, `DeclScanner` is
    still 100% under `declarationFromElement`). Lesson (again): verifying the *result* matches is not
    verifying the *cost* drops — for a "use a different API" change, confirm the expensive leaf
    disappears, not just that the output is identical.
    **Single-pass declaration map — IMPLEMENTED, MEASURED, REJECTED (defeated by deferred
    attribution).** Built a per-CU `IdentityHashMap<Element,Tree>` via one `TreeScanner` pass over
    `root` (recording each `VariableTree`'s `.sym → tree`, keyed exactly as `TreeInfo.declarationFor`
    matches), lazily on first variable query, invalidated in `setRoot`, with a scan fallback and a
    null-`.sym` skip. **Correctness:** after fixing a crash (the eager pass hit `VariableTree`s whose
    symbol is not yet set — `TreeUtils.elementFromDeclaration` *throws* on a null symbol; switched to a
    direct null-safe `.sym` read), full `:framework:test` + `:checker:test` pass. **But the warm-daemon
    A/B was flat** (treatment 130–142s vs baseline 131–133s). The traced run shows why: the single pass
    was cheap (`getRootVariableDeclarations` 11 samples) but **`DeclScanner` was still 1,327 samples,
    99.7% under `declarationFromElement` via the fallback** — i.e. the map was nearly empty. Root cause:
    **javac defers attribution of lambda/generic-method bodies**, so the variables that dominate the cost
    have *null symbols* when the single pass runs and are skipped; they get a symbol later, miss the map,
    and fall back to the full scan. And for the variables the map *does* miss, it is redundant with the
    existing per-element `elementToTreeCache` (one scan then cached either way). So pre-population can't
    win here — the expensive variables aren't attributed at any single build point. Reverted.
    **`declarationFromElement` smaller-scope scan — APPLIED in PR #1780.** Instead of
    `TreeInfo.declarationFor(sym, root)` (scan the whole CU), scan only the variable's *enclosing
    method/class* subtree: `TreeInfo.declarationFor(sym, trees.getTree(elt.getEnclosingElement()))`,
    falling back to the whole-CU scan if the enclosing tree is unavailable or does not contain the
    declaration. The key difference from the failed `trees.getTree(localVar)`: here `trees.getTree`
    is called on the **enclosing method** (cheap, position-based — the path the class/method/field
    case already uses), *not* on the local (which internally scans). It attacks per-scan *size*, so
    it sidesteps the attribution-timing problem that killed the single-pass map. Also short-circuits
    `TYPE_PARAMETER` to `null` (it was scanning the whole CU only to return null — ~8% of default-branch
    calls). **Correctness:** full `:framework:test` + `:checker:test` pass (the fallback covers any
    edge case where the enclosing subtree lacks the declaration, e.g. some initializer-block locals).
    **Performance (same-session traced A/B on full `checknullness`):** `declarationFromElement`
    1,695 → 1,139 (**−33%**), `DeclScanner` 1,652 → 1,099 (−33%), **total on-CPU 12,284 → 11,658
    (−5.1%)**; warm-daemon wall clock ~2–4% (noisy). The ~33% (not ~90%) reduction reflects that
    enclosing methods are a non-trivial fraction of their files plus some fallbacks; scoping tighter
    than the method (no element exists for a block) would need a different mechanism. This is the
    real lever on `declarationFromElement` that `trees.getTree` (relocates the scan) and the
    single-pass map (defeated by deferred attribution) both missed.
  - **Symbol completion is now small (~2.4%): largely solved.** `apiComplete` 1.46% + `ClassSymbol.complete`
    0.48% + `ClassFinder.complete` 0.48%. The earlier "1.50%/0.97% leaders" are gone (PR #1763 getKind
    overrides + Phase 1 cutting `createType` traffic). The `getKind`→completion hypothesis is *resolved*:
    `CFAbstractValue.canBeMissingAnnotations` does sit above `apiComplete` (31% of `apiComplete`, ~0.46%
    of total) and `createType` the rest — real but minor.
  - **Name decoding ~2.3%** (`Convert.utf2chars` 1.43% + `utf2string` 0.86%). The "annotation formatting
    in the hot path" question is *resolved, opposite to the prior guess*: 36% of `utf2chars` is under
    `DefaultAnnotationFormatter.isInvisibleQualified` (22%) + `AnnotationUtils.toStringSimple` (14%) — i.e.
    `ATM.toString`/`CFAbstractValue.toString` invoked **during type-checking, not the stub parser**.
    *Update (PR #1796):* the name-*comparison* share of this slice is addressed (interned-`Name`
    identity helpers + `sameName`); what remains is the *formatting/stringification* share — the
    unguarded `toString` was found (see the eager-error-formatting bullet below), plus
    `ProperType.computeHashCode` (hashes `toString()`) and `SourceChecker.shouldSkipUses`
    (`Symbol.toString()` per call for a regex match).
    *Update (PR #1797, June 2026):* the stringification share is now addressed — `FoundRequired`
    formatting is lazy, `shouldSkipUses` is cached, `ProperType`/`Variable` hash without
    `toString()`, `LocalVariableNode.hashCode`/`equals` read `Name` directly, and the annotation
    name maps use `IdentityHashMap<Name>`. Measured full-build (`./gradlew checknullness`)
    warm-daemon A/B: branch ~2m10s, master ~2m16–18s (~7s, consistently one-sided but near the
    5–10s noise floor — as expected for a ~0.9% utf2* share). JFR: `utf2chars` 0.57% +
    `utf2string` 0.32% = **0.89% combined** (down from the ~2.3% pre-#1796 level); remaining
    callers are cold stub-parsing paths, diagnostic-only formatting, and first-visit cache misses.

  *(The earlier pre-cache attribution of this phase was removed as superseded: symbol completion is
  now ~2.4% and largely solved, the `getKind()`→completion question is resolved per the bullet above,
  and the name-comparison forcers — `isConstructor`/`isEnumSuperCall`/`findElement` — were addressed
  by PR #1796.)*

- **Defaulting: fuse all defaults into one type-tree traversal — APPLIED in PR #1836 (June 2026).**
  `QualifierDefaults.applyDefaultsElement` scanned the whole type tree *once per
  default* (~9.3 scans/call, ~14% inclusive on Guava) — the maintainers' TODO ("only one iteration
  through the defaults should be necessary"). Now it does **one** traversal that applies every default
  at each node: `DefaultApplierElementImpl.scan` loops over a precedence-ordered default list (built by
  `fusedDefaultsFor`) calling the extracted per-node `applyOneAtNode`, and the type-variable/wildcard
  visitors descend *once* for all defaults. `addMissingAnnotation` only adds when the hierarchy is
  unannotated, so applying the defaults in order preserves precedence — behavior-preserving by
  construction. The two subtle cases are faithful: use-only defaults (`TYPE_VARIABLE_USE`,
  top-level-local `LOCAL_VARIABLE`) are applied at the use node and are no-ops at bound nodes, so
  always descending is safe; and a **parametric qualifier** is excluded from a type variable and its
  bounds via a sticky `inTypeVarBound` flag (replicating `visitTypeVariable`'s early return). Subsumes
  the earlier top-level-only-skip. **Behavior-preserving:** byte-identical diagnostics on all-systems
  (269 files); the full `:framework:test` + `:checker:test` (56 suites, incl. `SubtypingEncryptedTest`
  which exercises parametric qualifiers, plus Value/Nullness/Index/Units/Interning/Regex/Lock/…) all
  pass. **Perf — generics-heavy (Guava nullness, apples-to-apples JFR):** `applyDefaultsElement`
  13.80% → **~9.7% inclusive (−30%)**, `QualifierDefaults.annotate` 15.57% → **~11.7% (−25%)**; ~noise
  on all-systems where defaulting is not hot (the cost scales with field/param/return types being deep
  generics — exactly Guava). **Floor reached:** the residual `applyDefaultsElement` is now dominated by
  the per-node `applyOneAtNode` (each default is still *checked* at each node — the fusion only
  collapsed the *traversal*, ~9.3× → 1×, not the per-node application); cutting further needs per-node
  default filtering (skip defaults whose location can't match a node kind), diminishing returns. The
  fused list itself is also memoized — see the next entry (an early per-`DefaultSet` *content* cache
  looked useless/unsafe, but the correct structure was later built and measured).
- **Defaulting: memoize the fused default list — APPLIED (June 2026, branch `cpu-experiments`).**
  `fusedDefaultsFor` rebuilt the precedence-ordered list on every `applyDefaultsElement` call (one of
  the hottest paths: ~once per defaulted type). Instrumentation settled the shape: in a generics-heavy
  compile **99.8% of calls pass an *empty* scope `DefaultSet`** (63,376 / 63,485), and there are only
  **2 distinct `DefaultSet` *contents*** by value — the 6,086 "distinct" sets are per-element *empty*
  objects (`defaultsAt` allocates a fresh empty set per element to short-circuit its cache; they are
  content-equal). So the result is highly memoizable. Two-part cache: (1) an `isEmpty()` fast-path
  returns one of two shared constant lists (the code defaults, ±unchecked) — covers the empty case
  with no map and no hashing; (2) non-empty scopes go through an **identity-keyed**
  `IdentityHashMap<DefaultSet, List<Default>>` (×2 for conservative). **Identity, not content,
  keying:** a `DefaultSet` is mutated in place by `addElementDefault`, so a content/hashCode key would
  corrupt the map; `defaultsAt` returns a stable per-scope object shared across a scope's members, so
  identity hits well. All caches are cleared by `invalidateFusedDefaults()` from the three (and only)
  default-set mutators (`addCheckedCodeDefault`, `addUncheckedCodeDefault`, `addElementDefault`); a
  `fusedDefaultsCached` flag (set in `fusedDefaultsFor`) makes that a no-op while defaults are still
  being registered, before any cache is populated. The returned lists are shared read-only (the
  scanner only reads them). **Why the earlier reject was
  wrong:** the first attempt keyed on `DefaultSet` *identity* for *all* calls — useless, because the
  6,086 empty objects gave 6,086 keys; and a *content* key was dismissed as needing a per-call hash.
  The fix is the split (constants for empty, identity map for non-empty), which neither earlier framing
  found. **Measured (no-memo baseline vs memo, n=3, deterministic alloc):** unmarked Generic300
  −0.84%, many-fields −0.51%; **wall neutral** (within noise). It is an allocation-only win (~1%) —
  the list-build was never the CPU cost (the per-node scan is), so it does not move wall time. Kept
  because it is the morally-correct code (don't rebuild a constant 63k×), it is safe, and — crucially —
  **it scales with JSpecify** (next paragraph). Correctness: `:framework:test` (incl.
  `ValueUncheckedDefaultsTest`), `NullnessTest`, `NullnessNullMarkedTest` all green.
- **Why the memo matters under JSpecify — MEASURED (June 2026).** `@NullMarked`/`@NullUnmarked` are
  *aliased to* `@DefaultQualifier` (`NullnessNoInitAnnotatedTypeFactory`, `jspecifyNullMarkedAlias`),
  so they populate the **scope** `DefaultSet` via `defaultsAt`'s enclosing-element inheritance — every
  element under a `@NullMarked` scope gets a non-empty scope default. So as JSpecify spreads the
  empty-fast-path's coverage **shrinks** while the identity cache's grows. Measured with one
  class-level `@DefaultQualifier` (= the `@NullMarked` alias target): empty fraction **99.8% → 73.3%**,
  distinct *content* **2 → 3** (still a handful — every marked scope yields identical content). A
  fully-marked codebase trends empty → the library/JDK floor (unmarked external elements stay empty),
  so the non-empty identity cache carries the savings. The marked workload shows the **larger**
  allocation win (**−1.03% vs −0.84%**), still wall-neutral. Net: today the non-empty case is 0.2% of
  calls (not worth caching alone); under JSpecify it becomes the majority, and the distinct-content
  count stays tiny — exactly the regime where a result memo pays off. The split design is future-proof
  to marking density without betting on either case dominating.
- **Defaulting: drop the vestigial scanner type parameter — APPLIED (June 2026, branch
  `cpu-experiments`).** Cleanup the fusion exposed: `DefaultApplierElementImpl extends
  AnnotatedTypeScanner<Void, AnnotationMirror>`, but post-fusion the scanner reads the fused list from
  `outer.fusedDefaults`, never threading the per-default annotation as the scanner's `P`. Every
  `scan`/`visit*` override only carried an `unusedQual` and passed `null` down. The only non-null-`P`
  caller, the legacy `applyDefault(Default)` single-default method, had **zero callers and was in fact
  broken** (it set `location` and called `visit`, but `scan` reads `fusedDefaults`, which only
  `applyDefaults(List)` sets → NPE). Removed it and changed the scanner to `AnnotatedTypeScanner<Void,
  Void>` (three override signatures `AnnotationMirror unusedQual` → `Void unusedQual`). No subclasses
  of `QualifierDefaults` and no external references to `DefaultApplierElement`/`applyDefault` exist, so
  the change is local. Behavior-preserving by construction (the threaded values were always `null`);
  `applyOneAtNode`'s real `AnnotationMirror qual` (from the fused list) is untouched.
- **The defaulting walk is the largest *CF-controlled* leaf cluster — FEASIBILITY MEASURED (June
  2026), verdict: highly memoizable, worth building.** `QualifierDefaults.DefaultApplierElementImpl.scan`
  plus `AnnotatedTypeScanner.visitDeclared`/`scan`/`reduce` are the biggest type-factory leaf group.
  Note `QualifierDefaults.elementDefaults` *already* caches the per-element *DefaultSet*; the profiled
  cost is the *application* — `applyDefaultsElement` scans the whole type tree once per `Default`.
  Instrumented `applyDefaultsElement` on `:framework:checkNullness` (one fork, ≥3.0M calls, ~28M scans),
  keying each call on `(identityHashCode(scope), structural ATM.hashCode of the input type BEFORE
  mutation)` — a 64-bit composite, so hash-collision inflation is negligible at ~300k distinct keys:
  - **scans per call ≈ 9.32** — each call triggers ~9 full type-tree scans (one per default in the set
    + checked/unchecked-code defaults). High multiplier: a single cache hit elides all ~9 at once.
  - **repeat rate (same `(scope, input-type-structure)` already seen): tree-path 88.0%** (1.41M calls,
    168k distinct), **element-path 91.6%** (1.59M calls, 133k distinct). So defaulting is overwhelmingly
    *redundant recompute*, not use-site-unique — the core feasibility question is answered yes.
  - **Cost model favors a cache.** Per call a `(scope, structural-type)` cache costs ~1 `ATM.hashCode`
    walk (≈1 scan-equivalent) for the key + a deep-copy on a hit; amortized at ~90% hit that is ~2.9
    scan-equivalents/call vs. ~9.3 today — roughly a **3× cut** in defaulting work (defaulting is a
    single-digit-% slice of self-time, so expect a few % end-to-end; confirm with a full-build A/B).
  - **Refinement — split by path, because the two want different keys.** The **element path** (91.6%,
    `annotate(Element, type)` from `getAnnotatedType`) has an input type that is a *pure function of the
    element*, so it can be keyed on the **element identity (cheap)**, no structural hash needed. **NB
    (corrected): this redundancy is NOT `elementCache` eviction churn** — see the `elementCache`
    measurement below: `elementCache` already hits ~92%, but it stores the type *before* defaults
    (`fromElement`'s contract), and defaulting (`annotate(Element, type)`) runs *after* `fromElement` on
    every `getAnnotatedType` call regardless of the cache hit. So the element path needs its own
    **post-defaults** memoization (a new cache keyed on element identity), which enlarging `elementCache`
    would *not* provide. The **tree path** (88.0%, `annotate(Tree, type)`) has use-site-specific types
    and genuinely needs the **structural `(scope, type)` key**; it pays the uncached-`ATM.hashCode` key
    cost (the immutability-plan risk #2 — measure that the hash walk does not eat the win), but 9.3
    scans/call × 88% repeat says it still pays. Both paths are real; the tree path is the novel part.
  - **Soundness + validation.** Defaulting only *adds missing* annotations and is deterministic given
    `(scope, input-type-structure)`, so the structural repeats produce identical outputs; cache with
    copy-on-store/return, same recipe as the `asMemberOf`/`directSupertypes` caches. Validate via
    `alltests` **diagnostics**, never a recompute cross-check (the non-idempotency trap above).
  - **Honest bounds:** numbers are from *one* subproject (ratios should generalize, but the absolute %
    needs the full `checknullness` A/B); and the ~9.3 multiplier assumes each `applyDefault` ≈ a full
    scan — if some short-circuit, both the savings *and* the key/copy cost shrink together, so the
    favorable ratio is robust but the magnitude is not yet pinned.

- **`elementType` cache (Phase 1) — APPLIED in PR #1777.** Implemented the value-returning element-keyed
  cache: a new `AnnotatedTypeFactory.elementTypeCache` (`LRU(getCacheSize())`, deep-copy on
  store/return) memoizes the *fully-computed* `getAnnotatedType(Element)` result (post `fromElement` +
  `addComputedTypeAnnotations`, i.e. after type annotators + qualifier defaulting). A hit returns a deep
  copy and skips the whole pipeline. Cheap **element-identity** key (no `ATM.hashCode`); no poly guard
  needed (declaration defaulting does not resolve `@Poly` from arguments — like `directSupertypes`);
  overridable `shouldCacheElementType()` opt-out (default true) for checkers whose
  `addComputedTypeAnnotations(Element, …)` is not a pure function of the element. Not cleared between
  CUs (element-keyed, stable — same as `elementCache`). **Correctness:** full `:framework:test` +
  `:javacutil:test` + `:dataflow:test` + `:checker:test` pass (0 diagnostic failures) — no bundled
  checker needed the opt-out. **Performance — `≈10%` wall clock (worth keeping).** Mechanism (single
  `--no-daemon` back-to-back, full `./gradlew checknullness`): element-path defaulting roughly halved —
  `DefaultApplierElementImpl.scan` 361→247 (−32%), `DefaultApplierElement.shouldBeAnnotated` 135→63
  (−53%) — and the **type-factory phase dropped −15.2%** (5,594→4,742 samples). Wall clock, the metric
  that matters (warm-daemon, 3–4 reps/side, median): **baseline PR 1777 2m34s → isolated Phase 1 2m19s,
  ≈ −15 s / −10%, and the Phase-1 reps were tightly clustered (2m19s ×3–4) vs the baseline's 152–157 s
  spread.** This is a real, consistent win — **keep Phase 1.**
  **Two measurement traps this corrected (see "Measuring wall-clock effects" in the SKILL):**
  (1) my *first* read called it "≈2%/noise" — that was a single `--no-daemon` run; cold per-fork JVM
  startup dilutes the type-checking gain and a single run is noise-dominated. The warm-daemon
  multi-rep wall-clock is the reliable measure (≈10%). (2) An intermediate A/B that *mixed* Phase 1
  with a `directSupertypes`-cap experiment showed **zero** wall-clock change — because the two effects
  cancelled (see the next bullet). **Never A/B two changes at once.**
  **Phase 2 (tree path, structural `(scope, type)` key + write-back) deferred** per plan — re-profile
  after Phase 1 to see whether tree-path defaulting is still worth its write-back tax.

- **`constructorFromUse` cache (analog of `methodFromUse`) — IMPLEMENTED, MEASURED, REJECTED.** A
  tempting target: `constructorFromUse` is ~12% inclusive (even on the fully-cached branch), and a
  spike showed a **96.4% hit rate** on `(ctor, instantiated-type)` with only **1.7% anonymous**
  (skipped) and **~176 distinct keys** (so ~free on memory). Implemented the full cache (same recipe
  as `methodFromUse`: structural key, deep-copy on store/return, poly guard, `shouldCacheConstructorFromUse`
  opt-out defaulting to the method opt-out, anonymous-class carve-out, plus a `type.deepCopy()` on the
  *stored key* because the instantiated `type` can alias the returned constructor's in-place-mutated
  return type). **Correctness: full `:framework:test`/`:javacutil:test`/`:dataflow:test`/`:checker:test`
  pass (0 failures).** **But the warm-daemon wall-clock A/B (cache on vs off via the opt-out, Phase 1
  constant) showed NO benefit — 2m21s vs 2m19s, i.e. flat-to-slightly-negative.** Why the 96% hit rate
  didn't translate (the lesson): the deep-copy-cache **overhead floor** — a structural key hash
  (`type.hashCode()`, an uncached ATM walk) on *every* call + a deep-copy on hit + a deep-copy of the
  stored key ≈ 2 type-walks — roughly *equals* the work a hit saves, because the saved part is just the
  constructor `asMemberOf` (`getAnnotatedType(ctor)` is already Phase-1-cached) and constructors are
  **infrequent** (~5–10k calls), so the fixed overhead never amortizes. Contrast `methodFromUse`/
  `directSupertypes`, which save *more* than the tax per hit **and** fire far more often.
  **Takeaways:** (1) hit rate is necessary but not sufficient — always confirm with the wall-clock A/B;
  (2) a cache only wins when (per-hit saving − deep-copy tax) × frequency is positive, which immutability
  (removing the deep-copy tax) would change — so this could be worth revisiting *after* immutability,
  but not before. Reverted.

- **Do NOT shrink the heavy caches to save memory — MEASURED, the cap is worth ≈10% wall clock.**
  PR 1777's two LRU caches add **≈ +50–70 MB retained live heap** on a full `checknullness` (measured
  master vs branch via post-GC `jdk.GCHeapSummary` "After GC": median 207→259 MB, p90 358→426 MB; both
  caches fill to their 2000 cap; `directSupertypes` stores `List<AnnotatedDeclaredType>`, the heaviest
  per entry). That footprint is JDK-independent but caused **memory pressure on Java 8 CI specifically**
  (root cause: on Java 8 the `check*` tasks ran *in-process in the shared Gradle daemon* heap, not
  forked — fixed separately by forking them, PR #1778). The tempting fix — halve the cache size — was
  **tried and rejected**: `directSupertypes` at `cacheSize/2` (1000) is a **≈10% wall-clock regression**
  (the mixed Phase-1+`directSupertypes@half` A/B landed at 2m34s, i.e. the shrink gave back *all* of
  Phase 1's ≈15 s gain), far worse than its 90.5%→81% hit-rate delta suggested. Per-factory hit-rate
  vs cap (measured): `directSupertypes` 256/512/1024/2000 → 54.6/65.3/81.2/90.5%; `asMemberOf` →
  49.3/60.0/69.8/80.2%. **Conclusion: keep all caches at full `cacheSize`.** The right way to cut their
  memory without losing speed is reducing *per-entry weight* — the immutability program (shared frozen
  values, no `deepCopy`) — not reducing entry count. (`elementCache` rejected-unbounding note below is
  the dual: don't *grow* element caches either.)

- **`elementCache` unbounding / enlarging — MEASURED, REJECTED (not worth it).** Question raised: since
  `elementCache` is element-keyed, should it cache all elements (drop the `LRU(2000)`)? The "no limit
  for element keys" reasoning does *not* transfer: unlike the `Boolean`/`AnnotationMirrorSet`-valued
  element caches (`methodDeclaresPolyCache`, `cacheDeclAnnos`), `elementCache`'s value is a deep-copied
  full `AnnotatedTypeMirror` (heavy), and it is a shared base-class cache facing arbitrary downstream
  projects, so unbounding risks OOM on large builds. Instrumented the `fromElement` get/put on
  `:framework:checkNullness` (shadow LRUs at 2000 / 32000 / unbounded, aggregated across the nullness
  checker's factories): **real `LRU(2000)` already hits 91.9%; `LRU(32000)` and unbounded both hit 92.9%
  — only +1.0 pp — and they are *equal* because the largest single factory holds just 19,398 distinct
  elements, well under 32000, so unbounded buys nothing over a modest bump.** Verdict: not worth
  changing — a +1 pp hit-rate gain on a cache that already hits ~92%, against an OOM risk on large
  downstream projects (whose distinct-element count can exceed 32000, where unbounded *would* diverge).
  **Key correction this produced:** the element-path defaulting redundancy (above) is **not**
  `elementCache` eviction churn — `elementCache` stores the *pre-defaults* type and defaulting re-runs
  *after* `fromElement` on every `getAnnotatedType`, so enlarging `elementCache` would not reduce
  defaulting cost; the defaulting venue needs its own post-defaults cache.

- **`HashMap.getNode` (3.38% self) is flat and distributed — no single fix.** Nearest-CF split:
  `getDeclAnnotations` 27% (already cached in `cacheDeclAnnos`; the cost is the lookup itself, not a
  recompute), `isSupportedQualifier` 10.5% (a `Set<String>.contains` on the supported-qualifier names),
  `fromElement`/`getDeclAnnotation`/`declarationFromElement` the remainder. These are unavoidable map
  lookups on already-cached data; the only lever is reducing *call frequency* (fewer
  `getDeclAnnotations`/`isSupportedQualifier` calls per node), not the per-lookup cost. Low value as a
  direct target; better addressed indirectly if the defaulting/CF-into-javac venues reduce node visits.

- **Annotation *formatting* in the hot path — APPLIED in PR #1797 (June 2026).** Stack samples on
  the full `checknullness` build settled the "confirm before chasing" question: 148 samples (~1.1%)
  contained `AnnotatedTypeMirror.toString`, and the callers were **not** diagnostics-only. The two
  paths: (1) `BaseTypeVisitor.checkContainsSameToString` — a static `SimpleAnnotatedTypeScanner`
  whose lambda calls `type.toString()` *and* `type.toString(true)` on **every component of every
  type** — invoked via `containsSameToString` from `FoundRequired.of` and `shouldPrintVerbose`;
  (2) `reportCommonAssignmentError`/`reportMethodInvocabilityError`, which built `FoundRequired`
  (i.e. formatted both full types) **before** `checker.reportError`, so the formatting cost was
  paid even when the warning was subsequently suppressed. Fix: `FoundRequired.found`/`required`
  changed from `String` to lazy `Object` wrappers; `shouldPrintVerbose` result memoized. See the
  Applied optimizations entry above for the measured A/B.

- **CF driving javac internals — the biggest realistic CPU lever (~25% of total).** The
  wall-clock breakdown above attributes ~25% of all time to CF reaching into javac:
  forced `Symbol.complete`/`apiComplete` (from `getKind`/`createType`/
  `CFAbstractValue.canBeMissingAnnotations`/`getErased`/`ElementUtils.isTypeElement`),
  `Name`/UTF-8 decoding (`Convert.utf2chars`/`utf2string`, `Utf8NameTable.equals` — every
  time CF compares or stringifies a `Name` that isn't yet decoded/interned), and repeated
  `TreePath` construction/tree walks. PR #1763 (`getKind()` overrides), PR #1673
  (interned-name caching), PR #1796 (interned-`Name` identity comparison — removed the
  name-*comparison* share), and PR #1797 (lazy `FoundRequired` formatting, `shouldSkipUses`
  cache, `ProperType`/`Variable`/`LocalVariableNode` hash fixes, `IdentityHashMap<Name>`
  annotation maps — removed the stringification share; combined utf2* now **0.89%** on the full
  `checknullness` build) each chipped at one facet. This is bigger than dataflow + stubs
  + visitor combined and is the highest-leverage remaining CPU target for realistic
  compiles; it is incremental, not architectural — audit the remaining forcers/decoders
  that already have (or could cache) the needed info. Confirmed real, not the
  `assert`-guarded `validateSet` path (`:checker:checkNullness`'s forked javac runs without
  `-ea`).
- **Redundant type computation across the flow fixpoint (the 38%).** Flow analysis
  recomputes node types across iterations; the self-time is the type pipeline.
  Memoizing flow-insensitive node types within a run could help but is hard because
  of flow-sensitivity. Architectural.

Investigated and **rejected** this session:

- **Changing `TreeUtils.annotationsFrom*` to return `AnnotationMirrorSet`** (so the
  `addAnnotations` callers hit the index-based overload). Rejected: it is a public-API
  return-type break on `TreeUtils` (used by downstream checkers) rippling through ~15
  internal callers that declare the result as `List`, it shifts `List` (ordered,
  duplicates) to `Set` (dedups by `areSame`) semantics, and only ~2 of ~18 callers
  pass the result straight to `addAnnotations` — and both are cold per-tree
  construction paths. Net: large break + semantic risk to remove two cold iterators.
- **A new `AnnotationMirrorSet.singleton(anno)` factory** for the
  `addMissingAnnotations(Collections.singleton(x))` sites. Rejected in favor of the
  existing singular `addMissingAnnotation(x)` (committed): the singular method
  allocates *nothing*, whereas an `AnnotationMirrorSet` singleton allocates an
  `ArrayList`-backed set — heavier than the JDK's immutable singleton. Rule for future:
  a single annotation → the singular `add/addMissing/replaceAnnotation` method, never a
  one-element collection.

**PR #1798 — the immutability foundation, and why the allocation win is blocked (June 2026).** This is
the session that built the `freeze()` mechanism and tried to cash it in for the deep-copy-removal
allocation win. The foundation shipped; the allocation win did not, and the dead ends are precise and
worth not re-walking.

*What shipped (PR #1798), behavior- and perf-neutral.* A `frozen` bit on `AnnotatedTypeMirror`;
`checkMutable()` throwing `BugInCF` on the three primary-annotation sinks
(`addAnnotation`/`removeAnnotation`/`clearAnnotations` — every other annotation mutator routes through
them), with `primaryAnnotations.makeUnmodifiable()` as a backstop for the `getAnnotationsField()` and
`AnnotatedDeclaredTypeNoHierarchy.addAnnotation` paths; a cycle-safe deep `freeze()` (the `frozen` bit
is the visited marker) that freezes only already-initialized components, with the lazy getters
freezing components they create after the owner is frozen; and freezing the master stored at all eight
caches (`elementCache`, `elementTypeCache`, `classAndMethodTreeCache`, `from{Member,Expression,Type}TreeCache`,
`methodAsMemberOfCache`, `directSupertypesCache`). The caches still `deepCopy()` on every hit, so this
is behavior-neutral. **Structural setters are deliberately left unguarded** — the corruption vector is
annotation mutation, and deep `freeze()` already freezes every reachable component's annotations;
guarding the structural setters would need a raw-setter split of `BoundsInitializer` (the bound setters
are called on the frozen owner during lazy init) for no safety gain. **A/B (deterministic
`jdk.ThreadAllocationStatistics`, median of 3, + wall + on-CPU):** allocation **−0.09%** on a 300-method
generic file and **+0.07%** on a 400-vararg-method file (both within the ~0.15% band); `freeze()` does
not appear in 1,725 on-CPU samples on a 1500-method compile (<0.06%); wall within noise. The `frozen`
boolean adds no per-object allocation (it fits existing object padding — total allocation did not move).

*The flush traced to ONE copier bug, not pervasive aliasing.* Freezing the masters initially flushed
`MethodValInferenceTest` + ~12 `NullnessTest` cases as `BugInCF` ("Attempted to mutate a frozen
AnnotatedTypeMirror"), which *looked* like the construction pipeline embedding cached substructure
everywhere. It was a single bug: `AnnotatedTypeCopier.visitExecutable` did
`copy.setVarargType(original.getVarargType())` — **aliasing** the original's vararg `AnnotatedArrayType`
into the "copy" instead of copying it, so `deepCopy()` of an executable type was not fully independent
and shared its whole vararg subtree (`Object[]`/`Class<?>[]`/`LinkOption[]` and everything reachable —
which is why every flushed underlying type was an array or an array-subtree node). Defaulting then
mutated that shared subtree. Fixed with `copy.setVarargType((AnnotatedArrayType) visit(original.getVarargType(),
originalToCopy))` — the `originalToCopy` map returns the already-made parameter copy when the vararg is
the last parameter (the common case, so the fix is allocation-neutral), else a fresh copy. With that one
fix, freezing all eight masters is green on the full suite. **Lesson: when freezing flushes a cluster of
mutations, look for a shared copier/construction bug before assuming pervasive aliasing.**

*The aliasing was benign to results — the only symptom is the freeze crash.* The vararg type is consumed
read-only (`PropagationTreeAnnotator`, `BaseTypeVisitor`); its only post-copy mutator is qualifier
defaulting, which is idempotent (`addMissingAnnotation`), so the shared subtree always got the same
annotations and no wrong diagnostic ever resulted — which is why it was latent and master's suite was
green. Confirmed three ways on one program (JDK vararg calls `Arrays.asList`/`String.format`/`Class.getMethod`):
clean on master, `BugInCF` on freeze-without-fix, clean on freeze+fix. Consequence: the regression test
(`checker/tests/nullness/VarargCacheAliasing.java`, PR #1798) only demonstrates the bug *with* the freeze
enforcement present; a standalone fix would need a unit test asserting `deepCopy` independence. This is
also why PR #1798 keeps the fix and the freeze work in one change.

*The load-bearing-copy finding — four attempts, all confirming the cache-return `deepCopy` cannot just be
dropped.* The whole point of freezing masters was to then return the shared frozen instance and delete the
copy. It does not work, because the dominant consumers mutate what they get back:
- **Element-boundary flip** (`getAnnotatedType(Element)` returns the frozen master): flushed
  `DefaultInferredTypesApplier` (flow refinement, 60), `constructorFromUse` (`type = getAnnotatedType(elt);
  type.clearAnnotations()`, 25), `CommitmentTypeAnnotator`, `DefaultQualifierPolymorphism`, `ValueTreeAnnotator`,
  ... The results feed the always-mutating tree pipeline (`visitIdentifier`/`visitMemberSelect`/`asMemberOf`
  → `addComputedTypeAnnotations`), so fixing each site means a `deepCopy()` *before* the mutation — which
  **moves** the copy to the consumer, not removes it. The flip saves a copy only for read-only direct
  consumers, the minority.
- **`methodFromUse` copy-elision** (skip the on-hit `deepCopy` since `typeVarSubstitutor.substitute` copies
  again for generic methods): **type-argument inference mutates the method type in place** —
  `findTypeArguments` → `DefaultTypeArgumentInference.inferTypeArgs` →
  `typeinference8.Resolution.resolveWithLowerBounds` calls `replaceAnnotations` on a component of `preType`.
  So the pre-inference copy is load-bearing.
- **`hashCode` caching on frozen ATMs** (the perf-notes' standing "can't cache because mutable" item):
  instrumented **0.0% of `hashCode()` calls land on frozen types** (0 of 185k / 370k on the size sweep) —
  every hot hash target is a mutable working copy, because the caches return copies. Worthless in the
  current architecture, and doubly blocked (it would need the boundary flip, which itself does not pay).
- **Shallow-location defaulting shortcut** (skip the recursive descent for the top-level-only locations
  FIELD/PARAMETER/RETURN/RECEIVER/RESOURCE_VARIABLE/EXCEPTION_PARAMETER/CONSTRUCTOR_RESULT — but NOT
  LOCAL_VARIABLE, which has a type-variable-use special case): cut scan calls only **10.2%** (586k→527k),
  and those saved scans are over cheap shallow types (`Object` parameters); **allocation flat**. The
  expensive defaulting is the deep `OTHERWISE` + bound-location traversals over generic types, which the
  shortcut does not touch. (Separately measured: `addMissingAnnotation` is **74% no-op**, and
  `applyDefaultsElement` does N full scans, one per `Default` — so a *full* single-pass merge could pay,
  but it is a high-risk refactor of the recursive bound logic with a ~2% ceiling.)

**Where this leaves the program.** A boundary flip *is* achievable — but it relocates the copy to each
mutating consumer rather than removing it, so the realized win is only the read-only-consumer fraction.
Two findings refine the earlier "blocked" verdict:

- **The cross-cutting blocker was one latent bug, now fixed (PR #1798): side-effecting equality.**
  `ValueAnnotatedTypeFactory`'s `arePrimaryAnnosEqual` override normalized its operands by mutating them
  (`replaceAnnotation`) before comparing. That fired during *every* cache flip (it runs in subtyping/equality,
  which all cache results flow through). Made non-mutating (compute the canonical annotations, compare without
  mutating). It is the prerequisite for any flip and a correctness fix on its own.
- **`classAndMethodTreeCache` flip shipped (PR #1798) — green, but modest.** With the equality fix plus
  copy-on-frozen at the ~6 mutating consumers it flushed (`getMethodReturnType`, `getSelfType`, the
  `getAnnotatedType(Tree)` pipeline choke-point, `constructorFromUse`'s enclosing type, and
  `ValueVisitor.checkOverride`), the flip is green on the full suite. **But deterministic A/B is ~−1% on a
  method-heavy file and ~0% on realistic code — `classAndMethodTreeCache` is low-volume.** Shipped for
  GC-relief + to establish the copy-on-frozen consumer-fix pattern.
- **The high-volume `elementTypeCache` is mutation-dominated, so likely also modest.** Its flip flushed
  108 events; its dominant consumer is `asMemberOf` (every method call via `methodFromUse`), which mutates the
  result (poly resolution, substitution, `postAsMemberOf`). Flipping it needs `asMemberOf` to copy-on-frozen
  on its alias-return paths, which moves the copy back — limiting the win to read-only element-type queries.
  Not pursued: large fix set, likely-modest win.

So the per-cache lesson: the flip is *mechanically* unblockable (copy-on-frozen at the enumerated mutating
consumers; the freeze tripwire makes it incrementally safe), but the high-volume caches' hot consumers mutate,
so the realized win is small. The larger allocation win still needs **copy-on-write** (mutator returns a fresh
node sharing unchanged children — though the whole-tree re-annotators like defaulting/flow get no benefit) or
**eliminating redundant re-annotation** (Defaulting Phase 2). Prototype + JFR-A/B before more flips. The
higher-leverage perf target remains CF→javac internals (see the open venues).

#### Open venues (current — global trace after the smaller-scope `declarationFromElement` scan)

A fresh full-`checknullness` trace (11,009 on-CPU samples; javac internals 35.8% / type factory 34.6%)
has a **flat leaf profile (no leaf > ~3%)** — the per-leaf hot spots are mined out. Remaining
CF-controllable clusters and their state, highest-leverage first:

1. **Immutability program — foundation + first flip shipped (PR #1798); remaining win small per cache.**
   Shipped: the `freeze()` mechanism, the `AnnotatedTypeCopier` vararg-aliasing fix, freezing all eight
   cache masters, the non-mutating-equality fix, and the first boundary flip (`classAndMethodTreeCache`
   returns the shared frozen value, with copy-on-frozen at its mutating consumers). The flip is **green but
   ~−1%/~0%** (low-volume cache). The flip technique is mechanically unblockable (copy-on-frozen at the
   enumerated mutating consumers), but the high-volume caches' dominant consumers mutate (`elementTypeCache`
   → `asMemberOf`), so their realized win is also likely modest. The larger win needs copy-on-write or
   eliminating redundant re-annotation, *not* more boundary flips — see the narrative ("Where this leaves
   the program") and Tried and rejected. Re-open with a copy-on-write prototype, measured.
2. **Defaulting Phase 2 (tree-path memoization).** Measured 88% `(scope, type)` repeat on the tree path,
   ~9.3 scans/call; per-CU clearing bounds the memory. *Gate on a within-CU-repeat measurement first*,
   and note it carries the same write-back tax that sank the `constructorFromUse` cache (real flat-risk).
   PR #1798 also measured the cheaper cache-free variant (a "shallow-location" shortcut) and found it
   negligible — see Tried and rejected; the deep `OTHERWISE`/bound traversals are where the cost is, and
   merging those is the risky part.
3. **`getPath` / TreePath construction (~3.2%) — largely addressed by PR #1786 + #1788.** 68% of
   `TreePath.<init>` was under `AnnotatedTypeFactory.getPath`'s slow path (uncached
   `TreePath.getPath(root, tree)` scans on cache miss + heuristic failure). PR #1786 caches the
   per-body lookup; PR #1788 makes `TreePathCacher` lazy and routes `getPath` through it, removing
   most of that allocation. **Residual — RESOLVED by PR #1789.** A single class with very many
   methods still allocated *super-linearly* after #1786/#1788 (1500 methods 4.9 GB → 3000 11.8 GB →
   6000 32.1 GB, ~2.5–2.7× per doubling). An `alloc`-by-nearest-CF-frame capture (via a
   `gen-sized-program.py` size sweep) traced it to `getPath` searches that rescanned the whole class
   per lookup; PR #1789 starts those searches from the tightest known path, making it linear (6000
   methods 32.1 GB → 14.8 GB). See "Linear `getPath` searches" in Applied optimizations.
4. **`declarationFromElement` residual (~5–7%).** Still the largest single javac-interaction cost after
   the smaller-scope scan; residual is method-subtree scanning. The cheap levers are exhausted (scoping
   tighter than a method has no element; `trees.getTree` and the single-pass map were rejected).
5. **Small / blocked:** `ElementUtils.qualifiedNameCache` (`synchronizedMap`+`WeakHashMap` lock/expunge,
   ~0.58%, blocked on a thread-reachability + daemon-memory audit — see Short list above). Annotation
   formatting in the hot path is now **resolved** (PR #1797 lazy `FoundRequired`); remaining utf2*
   at 0.89% is cold-path / first-visit-miss only.

**Copy-on-write ATMs — PROTOTYPED (June 2026, un-merged experiment): solves the soundness blocker,
allocation win real, but wall-clock-negative.** Recorded in PR #1835; the COW code lives on branch
`cow-prototype` (a PR was opened but will not be merged — kept as a reference implementation). The
blocker (above) was that returning a shared frozen cache master crashes
when a consumer reparents a frozen *child* into a fresh non-frozen result and mutates it (root-level
`deepCopy()` guards can't catch a non-frozen root holding a frozen child; Guava found what `alltests`
+ 9 fixes missed). A working COW prototype was built (branch `cow-prototype`, gated by `-Dcf.cow`):
the six post-pipeline caches (`elementType`, `element`, `fromMember`, `fromExpression`, `fromType`,
`methodAsMemberOf`) return `cowCopy()` — a non-frozen `shallowCopy()` that shares the master's frozen
children — instead of `deepCopy()`; the ~13 child accessors (`getUpperBound`/`getTypeArguments`/…)
**plus the three `fixupBoundAnnotations`** (which mutate bound *fields* directly, bypassing the
accessors — the second class of reparenting path) lazily unshare a frozen child of a non-frozen parent
(`cowChild`/`cowChildren`), so a mutation copies only the spine it touches and a read-only hit copies
one node. A per-node `cowDirty` flag (set by `cowCopy`, checked in the accessors) keeps the COW scan
off the hot path for the majority of (non-cache) types.
- **Soundness: complete and validated.** Passes the regression test
  `ElementTypeCacheWildcardBound`, all-systems (269 files, byte-identical diagnostics to COW-off),
  and — the decisive test — a **full Guava nullness build (BUILD SUCCESS, 0 crashes)**, the venue that
  caught the original flip crash. COW-on-access is the complete fix the narrative predicted: it makes
  all six caches flippable and the whole reparenting bug class disappears. Two non-obvious lessons:
  (a) the executable type's `getTypeVariables` and the union/intersection `getAlternatives`/`getBounds`
  are easy accessors to miss; (b) the `fixupBoundAnnotations` field-level mutation is a *second*
  reparenting surface that COW-on-access alone (intercepting only the public getters) does not cover —
  it must be routed through `cowChild`/`cowChildren` too.
- **Allocation: −4.8% (real).** Deterministic `ThreadAllocationStatistics`, all-systems 269, median of
  5: 5709.6 → 5434.3 MB; loops −3.6%. Larger than the ~1% the old `elementType`+`classAndMethod`
  boundary flips got, because COW flips all six caches and `shallowCopy` is far cheaper than `deepCopy`
  on read-only hits.
- **Wall clock: +5.3% (regression).** all-systems 269, min of 3: 19.81 → 20.86 s. The per-access COW
  machinery costs more CPU than the allocation reduction returns. The `cowDirty` flag fixed the one hot
  frame (`cowChildren` 3.74% → 0.50% self-time), but the residual is **diffuse** — the distributed
  per-access checks plus per-child `cowCopy` allocations across the walk (note `shallowCopy` for
  type-variables/wildcards is itself a `deepCopy`, so a generics-heavy type deep-copies each bound on
  access). This confirms the post-mortem: by now the copier is cheap, so eliminating it does not pay in
  wall clock — COW is an **allocation / GC-pressure** win (valuable at scale, on tight heaps), not a
  single-compile wall win.
- **Wall optimization attempted — COW cannot be made wall-positive (it is a memory win, not a wall
  win).** Two levers tried (branch `cow-prototype`, second commit): (1) gate every child accessor with
  `cowActive()` (`= COW && cowDirty`) so non-cache types skip the `cowChild` call and the field write —
  removed the per-access *write* but did not move wall, so the write was not the cost; (2) flip **only
  `elementTypeCache`** (the one cache with read-only-majority consumers, 65–88%) and revert the
  full-walk caches (`element`/`fromMember`/`fromExpression`/`fromType`/`methodAsMemberOf`) to
  `deepCopy`. Results (all-systems 269): all-six COW = alloc −4.8% / wall +5.3%; elementTypeCache-only
  = alloc −0.2% / **wall +1.8% (noise)**. **No configuration is wall-positive**, for two structural
  reasons: (a) the copier is already cheap (~1–2% — the post-mortem above), so eliminating it cannot
  beat the *global per-accessor `cowActive()` tax* COW imposes on every type, not just cache results;
  and (b) the cache consumers (defaulting/annotators) **fully walk** the result, so the read-only-skip
  benefit never materializes and piecemeal per-child `cowCopy` is slower than one batched `deepCopy`.
- **Downstream / GC-bound win? — TESTED, no.** Hypothesis: the −4.8% allocation has no wall value on a
  roomy single compile (GC ≈4%) but should pay off on a memory-pressured build. Heap sweep (all-systems
  269, all-six COW): the wall penalty *shrinks* as the heap tightens (−Xmx 512m **+8.1%**, 320m +0.5%,
  256m **+0.1%**) — which looks like the GC story — **but a clean GC measurement at −Xmx256m shows COW
  does not reduce GC**: 0.77 s vs 0.76 s pauses, **225 collections both**. So the gap-closing is
  tight-heap measurement variance (the *baseline* slows), not a COW GC saving — the −4.8% allocation
  does not convert to fewer collections. Root cause: **CF compilation is CPU-bound** (~96% on-CPU, GC
  ≤4% even on the large `checknullness` build), so the GC ceiling is ~4% and COW captures ~none of it
  (−4.8% alloc → ≈−0.2% wall), nowhere near the +5% CPU tax. (COW reduces transient *churn*, not
  *retained* heap — the cache masters are unchanged — so it does not relieve the OOM/footprint pressure
  either; that needs per-entry-weight reduction, the original immutability goal.) No downstream timing
  win.
- **Verdict.** COW is the **correct, complete solution to the soundness blocker** (Guava-validated) and
  delivers the **allocation win** (−4.8%), so it is the right tool if the goal is GC pressure / peak
  memory at scale or a clean immutable end-state. It is **not** a wall-clock win — for wall, the
  existing `deepCopy` is already optimal. Branch `cow-prototype` is kept as the reference
  implementation. A wall win in this region, if one exists, is not here (copier already harvested) —
  it is in *not producing* the types (the `getAnnotatedType`-redundancy family, already closed), not in
  copying them more cheaply.

---

## Reproducing measurements

Use [`checker/bin-devel/record-jfr.sh`](../../checker/bin-devel/record-jfr.sh)
for trace capture and
[`.claude/skills/cf-performance/jfr-analyze.java`](../../.claude/skills/cf-performance/jfr-analyze.java)
for analysis; see
[`.claude/skills/cf-performance/SKILL.md`](../../.claude/skills/cf-performance/SKILL.md)
for the analysis pipeline and the known pitfalls (the silent 10 ms
`MIN_SAMPLE_PERIOD` floor, Maven multi-module filename handling). Always
re-capture on the same workload after applying a patch to confirm the
targeted self-time percentage moved. A patch that passes tests but doesn't
move the profile is wrong by definition.

Three tooling-reliability bugs were found and fixed in June 2026 while
auditing whether the profiler gave trustworthy data; all three silently
produced misleading traces:

- **`stackdepth` was being ignored.** It was passed inside
  `-XX:StartFlightRecording=`, where it is not a valid option (the JVM
  warns `The .jfc option/setting 'stackdepth' doesn't exist.` and falls
  back to depth 64). It is a `-XX:FlightRecorderOptions` option and must be
  set there; `record-jfr.sh` now does.
- **Same-filename clobbering.** `JAVA_TOOL_OPTIONS`/`GRADLE_OPTS` reach
  every JVM the build spawns (launcher, daemon, test worker, forked javac).
  Pointing them all at one `filename=` made them overwrite each other and
  corrupt the constant pool — traces came back with `<null>` thread names
  and a leaderboard dominated by the launcher's idle frames
  (`EPoll.wait` ~49%, `ProcessHandleImpl.waitForProcessExit0` ~21%) rather
  than type-checking. `record-jfr.sh` now uses the JFR `%p` filename token
  so each JVM writes its own file; the largest is the worker.
- **`jfr print`/`jfr view` crash on JDK 25** with a
  `StringIndexOutOfBoundsException` in `ValueFormatter.formatMethod` /
  `PrettyWriter.formatMethod`, making the documented `jfr view hot-methods`
  pipeline unusable. `jfr-analyze.java` reads the recording via
  `jdk.jfr.consumer.RecordingFile` and avoids the broken formatter. It also
  computes self-time from `jdk.ExecutionSample` only — including
  `jdk.NativeMethodSample` floods the leaderboard with idle native frames.
