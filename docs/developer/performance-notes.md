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
- **(pending review)** — *Index-based iteration of `AnnotationMirrorSet`
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
- **Re-measured June 2026** — `reset()` uses `new IdentityHashMap<>(VISITED_NODES_EXPECTED_MAX_SIZE)`
  rather than `clear()`. Leaf-frame self-time on `allNullnessTests -PmaxParallelForks=1`:
  `IdentityHashMap.clear` = 3.42% (668/19479 samples); `IdentityHashMap.init` net after
  background subtraction ≈ 1.27% (456 total − 180 background = 276 samples, /20809).
  `clear()` wins on object allocation (1.09% vs 1.48% of TLAB events) but loses on CPU:
  `IdentityHashMap.clear()` walks all 128 table slots explicitly in Java; TLAB allocation
  uses JVM bulk zeroing. The pre-sizing in PR #1671 is what makes `clear()` more expensive —
  pre-sizing enlarged the array that must be explicitly zeroed.
- **PR #1763** — *Pre-sized `ArrayList` copies in `AnnotatedTypeCopier`.* Replaced
  `CollectionsPlume.mapList` lambda calls with direct pre-sized `new ArrayList<>(size)` loops.
  Removes lambda-dispatch overhead and allocates the destination list at the correct capacity
  immediately, avoiding internal growth copies.
- **(pending review)** — *Reuse the `QualifierDefaults` defaulting scanner instead of
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
- **(pending review)** — *Reuse two more per-use scanners: `TypeVarAnnotator` and the
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

- **(pending review)** — *Share the annotated-JDK stub AST across compilations.*
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
- **(pending review)** — *Avoid the defensive deep copy in read-only
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

---

## Tried and rejected

Bring new evidence before revisiting any of these — a JFR trace on a
workload not previously considered, or a measurement that contradicts
the prior finding. A fresh hypothesis is not new evidence.

- **`AnnotatedTypeMirror.getEffectiveAnnotations` caching.** JFR-
  attributed self-time was ~0.05% on the alltests trace and ~0.1% on
  the Oscar EMR (~4000 file) trace. Not a hotspot.
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

---

## Short list

Candidates raised in profiling sessions but not yet implemented. Capture
format: hot method, hypothesis, blockers/open questions.

A May 2026 review closed out every item previously on this list:

- **`TypeKind` as a field on `AnnotatedTypeMirror`** — *superseded, do not
  implement.* The goal (avoid the heap hop through `underlyingType.getKind()`)
  is already met by a cheaper mechanism: every subclass whose kind is constant
  (`AnnotatedDeclaredType`, `AnnotatedArrayType`, `AnnotatedExecutableType`,
  `AnnotatedTypeVariable`, `AnnotatedNullType`, `AnnotatedWildcardType`,
  `AnnotatedIntersectionType`, `AnnotatedUnionType`) overrides `getKind()` to
  return the constant inline. That costs zero memory and zero indirection,
  strictly better than the proposed ~8 MB field. Only `AnnotatedPrimitiveType`
  and `AnnotatedNoType` fall through to the base method, and for them
  `underlyingType.getKind()` is cheap and does not force symbol completion (see
  the doc comment on the base `getKind()`).
- **Pre-sizing `AnnotatedTypeCopier`'s per-visit map** — *already done.*
  `AnnotatedTypeCopier.visit` constructs its `IdentityHashMap` with
  `AnnotatedTypeScanner.VISITED_NODES_EXPECTED_MAX_SIZE` (64), the same
  pre-sizing as `AnnotatedTypeScanner.visitedNodes`.
- **`==` fast-path in `isSupportedQualifier`** — *rejected;* see the Tried and
  rejected section for the measurement.

The items above were each closed during the May 2026 review. One new candidate
is currently open; add others below as profiling surfaces them, with the
capture format above.

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

A June 2026 inclusive-time / co-occurrence investigation (looking for
*architectural* redundancy rather than leaf hot spots, since the leaf profile is
now flat) surfaced two further candidates. Both are blocked by correctness
invariants, which is why the campaign left them:

- **`AnnotatedTypeMirror.directSupertypes()` recomputes on every call.** It runs
  `SupertypeFinder.directSupertypes(this)` and wraps the result in a fresh
  `Collections.unmodifiableList` each time, with no per-instance cache; in a single
  real compilation (`checkNullness`) it is ~11% inclusive, and the lazy JDK-stub
  loading cascade (see below) calls it on the same classes at several recursion
  levels. **Blocker:** a declared type's supertype *annotations* depend on the
  type's own primary annotations, which defaulting and flow refinement mutate after
  the type is created, so a naive per-instance cache would hand back stale
  supertypes. A safe version needs either copy-on-write annotation sets or an
  invalidation hook tied to annotation mutation — i.e. it rides on the larger
  "make ATMs immutable" work, not a standalone patch.
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

Open venues, roughly by tractability:

- **Reduce ATM deep copying (`AnnotatedTypeCopier.visit` = 22% of `Object[]`).**
  Defensive deep copies exist only because ATMs are mutable (every `fromElement`
  cache hit, many `getAnnotatedType` paths). `ATF.getElementAnnotations` (committed)
  was a one-caller nibble. The real lever is **copy-on-write annotation sets or
  immutable ATMs**; this also unblocks the `directSupertypes` cache above. Large,
  architectural, high value.
- **`IdentityHashMap` pre-sizing (52% of `Object[]`, from `reset()`).** Pre-sized to
  64 → a 256-slot `Object[]` per scan, most visiting 1–3 nodes. *Settled* for
  realloc-vs-`clear()` and pre-size-64 (see the applied "Re-measured June 2026"
  note), but those measurements were CPU self-time on `allNullnessTests`. The
  realistic worker shows `Object[]` at 61% of allocations, so the **GC** side of the
  tradeoff is heavier than when last measured. Only revisit with *wall-clock + GC
  pause* data on a realistic compile (e.g. an adaptive/smaller initial size, or
  size-aware reset) — allocation-count alone will not overturn the prior CPU finding.
- **Forced javac symbol completion (`Symbol.complete`/`apiComplete` ≈ 3.3% self).**
  Driven during flow analysis by `getKind`/`createType`/`CFAbstractValue.canBeMissingAnnotations`/
  `getErased`/`ElementUtils.isTypeElement`. PR #1763 started this with constant
  `getKind()` overrides. Confirmed real (not the `assert`-guarded `validateSet` path:
  `:checker:checkNullness`'s forked javac runs without `-ea`). Incremental; audit
  remaining forcers that already have the needed info cheaply.
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
