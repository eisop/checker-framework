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
- **PR #1765** — *`BinaryOperation.hashCode` symmetry fix.* For commutative operations,
  `equals()` ignores operand order; the hash code must match. Replaced the
  order-dependent `Objects.hash(kind, left, right)` with
  `Objects.hash(kind, left.hashCode() + right.hashCode())` so that `a OP b`
  and `b OP a` hash identically. This is a correctness fix for the `equals`/`hashCode`
  contract that also improves cache hit rates for commutative expressions.

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

---

## Tried and rejected

Bring new evidence before revisiting any of these — a JFR trace on a
workload not previously considered, or a measurement that contradicts
the prior finding. A fresh hypothesis is not new evidence.

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

---

## Short list

Candidates raised in profiling sessions but not yet implemented. Capture
format: hot method, hypothesis, blockers/open questions.

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
- **`MethodInvocationNode.hashCode()` uses `Objects.hash(target, arguments)`.** That is the
  varargs `Object[]` + autoboxing antipattern called out in Applied optimizations ("avoid
  `Objects.hash`/`Arrays.hashCode` on hot paths") — each call allocates an `Object[]`. `Node`
  hash/equals back the dataflow worklists and stores, so it *may* be hot; but most dataflow maps are
  `IdentityHashMap` (identity, not structural), so first confirm with a JFR/alloc capture that
  `MethodInvocationNode.hashCode` is actually reached. If so, replace with `31 * target.hashCode() +
  arguments.hashCode()` (the field is immutable, so it could be precomputed). Spotted while auditing
  `MethodInvocationNode` during the PR #1788 session; not yet measured.

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

Open venues, roughly by tractability:

- **Reduce ATM deep copying (`AnnotatedTypeCopier.visit` = 22% of `Object[]`).**
  Defensive deep copies exist only because ATMs are mutable (every `fromElement`
  cache hit, many `getAnnotatedType` paths). `ATF.getElementAnnotations` (committed)
  was a one-caller nibble. The real lever is **copy-on-write annotation sets or
  immutable ATMs**; this also unblocks the `directSupertypes` cache above. Large,
  architectural, high value. **This is now an active staged program — see
  "AnnotatedTypeMirror value-semantics program" below.**

### AnnotatedTypeMirror value-semantics program + cache campaign (narrative; June 2026)

This subsection is the **detailed methodology log** for the cache campaign and the (still-open)
immutability program. Canonical statuses live in the top-level sections; this is the "how we got
there" record. **Status map:**
- **Shipped** (see Applied optimizations): the `methodAsMemberOf`, `directSupertypes`, and
  `elementType`/Phase-1 caches (PR #1777); the smaller-scope `declarationFromElement` scan (PR #1780).
- **Tried and rejected** (see that section): `constructorFromUse` cache, poly-deferral,
  `declarationFromElement` via `trees.getTree`/single-pass-map, shrinking the heavy caches.
- **Open** (see "Open items" at the end of Short list): the immutability program itself (delete
  `deepCopy`), and Defaulting Phase 2 (tree-path memoization).

Goal of the immutability program: make ATMs effectively immutable / copy-on-write so
`deepCopy`/`shallowCopy` can be deleted and the cache boundaries stop paying the deep-copy tax
(`AnnotatedTypeCopier` ~2% self-time + the dominant share of `Object[]` allocation) — and the
+50–70 MB the shipped caches retain goes away. Staged, each stage `alltests`-gated and JFR-measured;
full design in the session plan. **This is the recommended next direction** — it is the one lever the
evidence positively points to (the shipped caches proved valuable *and* proved the deep-copy cost),
now that the per-leaf profile is flat.

**Validation spike (DONE, GO).** A throwaway `methodFromUse` cache (non-generic methods, key
`(methodElt, structural receiverType, inferTypeArgs)`, copy-on-store/return) on
`:checker:checkNullness`: **66.7% hit rate**, `AnnotatedTypes.asMemberOf` (12.4% inclusive)
eliminated on hits, net allocation down even with the copy-tax, ~5% fewer on-CPU samples; the
structural-key hashing and copy-tax stayed below the inclusive threshold (cheap). Payoff confirmed.

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
    `ATM.toString`/`CFAbstractValue.toString` invoked **during type-checking, not the stub parser**. Worth
    a look for an unguarded `toString`, but small (~0.5%).

  Stale pre-cache attribution kept below for history:

- **CF driving javac internals (pre-cache attribution, superseded by the re-measurement above) is now
  the largest phase (33.6%) and the highest-leverage target.**
  This subsumes the "CF driving javac internals (~25%)" bullet below, which is still accurate but was
  measured pre-cache; the caches shrank the type-factory phase around it, so it is now proportionally
  larger. Concrete forcers seen in this trace, by `jfr-analyze.java under`/nearest-CF attribution:
  `Symbol.complete`/`apiComplete` (1.50%/0.97% self) are reached from `AnnotatedTypeFactory.createType`,
  `CFAbstractValue.canBeMissingAnnotations` (it sits on the stack above `Symbol.complete` via its
  `typeMirror.getKind()` chain at `CFAbstractValue.java:153–161`; the static overload is called
  directly from the `mostSpecific`/`leastUpperBound`/`greatestLowerBound` dataflow merges at
  :301/:573/:717, *not* only the `assert`-guarded `validateSet` at :110 — confirmed, the forked javac
  runs without `-ea`. **Hypothesis, not verified:** that `getKind()` itself forces completion — javac
  `Type.getKind()` is usually cheap, so the completion may be triggered by a sibling call in the merge
  and merely co-sampled; confirm the exact forcer before optimizing here),
  and `ElementUtils.isTypeElement`/`overriddenMethods`. `Convert.utf2chars` (1.29% self) is `Name`/UTF-8
  decoding; its nearest-CF callers split between legitimate stub work (`AnnotationFileParser.findElement`)
  and `TreeUtils.isConstructor`/`isEnumSuperCall`. Incremental, not architectural: audit each forcer for
  info it already has (e.g. a `TypeKind` it could read without completing the symbol, or an interned
  `String` it could compare instead of decoding a `Name`).

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

- **Annotation *formatting* in the hot path — confirm before chasing (likely a non-issue).**
  `AnnotationUtils.toStringSimple` and `DefaultAnnotationFormatter.isInvisibleQualified` appear under
  `Convert.utf2chars`, which would be alarming (formatting should only run for diagnostics). But
  `isInvisibleQualified` is called *only* from `DefaultAnnotationFormatter.formatAnnotationString`
  (ATM `toString`), and `toStringSimple` from `CFAbstractValue.toString`/`toStringSimple` and the stub
  parser — and the co-located nearest-CF frame is `AnnotationFileParser.findElement`, i.e. the **stub
  parser** legitimately decoding/matching annotation names, not type-checking. Before optimizing,
  confirm with a stack sample whether any `ATM.toString`/`CFAbstractValue.toString` is invoked from a
  non-diagnostic path (an unguarded message build); if it is only the stub parser, there is nothing to
  fix here. Sub-0.5% either way — low priority.

- **CF driving javac internals — the biggest realistic CPU lever (~25% of total).** The
  wall-clock breakdown above attributes ~25% of all time to CF reaching into javac:
  forced `Symbol.complete`/`apiComplete` (from `getKind`/`createType`/
  `CFAbstractValue.canBeMissingAnnotations`/`getErased`/`ElementUtils.isTypeElement`),
  `Name`/UTF-8 decoding (`Convert.utf2chars`/`utf2string`, `Utf8NameTable.equals` — every
  time CF compares or stringifies a `Name` that isn't yet decoded/interned), and repeated
  `TreePath` construction/tree walks. PR #1763 (`getKind()` overrides) and PR #1673
  (interned-name caching) each chipped at one facet. This is bigger than dataflow + stubs
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

#### Open venues (current — global trace after the smaller-scope `declarationFromElement` scan)

A fresh full-`checknullness` trace (11,009 on-CPU samples; javac internals 35.8% / type factory 34.6%)
has a **flat leaf profile (no leaf > ~3%)** — the per-leaf hot spots are mined out. Remaining
CF-controllable clusters and their state, highest-leverage first:

1. **Immutability program (recommended next).** The deep-copy tax (`AnnotatedTypeCopier` ~2% self-time
   + dominant `Object[]` allocation share) is what the shipped caches pay and is the +50–70 MB retained
   heap. The one lever the evidence positively supports; large/staged. See the narrative header above.
2. **Defaulting Phase 2 (tree-path memoization).** Measured 88% `(scope, type)` repeat on the tree path,
   ~9.3 scans/call; per-CU clearing bounds the memory. *Gate on a within-CU-repeat measurement first*,
   and note it carries the same write-back tax that sank the `constructorFromUse` cache (real flat-risk).
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
   ~0.58%, blocked on a thread-reachability + daemon-memory audit — see Short list above); annotation
   formatting in the hot path (~0.5%, confirm it is type-checking not the stub parser before chasing).

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
