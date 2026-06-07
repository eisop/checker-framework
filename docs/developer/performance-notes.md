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

### `AnnotationMirrorSet` and annotation utilities

- **PR #1649** — *Reimplement `AnnotationMirrorSet` using an
  `ArrayList`.* Sets are small in practice; `TreeSet`'s `compareTo`
  (which decodes `Name` to `String` per comparison) was strictly more
  expensive than linear `areSame` on the observed sizes. Removed
  `NavigableSet` from the public interface — see CHANGELOG note. The
  patch initially shipped with a regression in `addAll` semantics; the
  fix preserves the non-standard fast-path return-`true`-if-any-new
  contract.
- **PR #1669** — *Improve equality and comparisons of annotation
  names.* Introduced `AnnotationUtils.annotationNameAsName`, which
  returns the underlying `Name` without ever allocating a `String`. Hot
  callers that only need identity comparison or hashing now go through
  it. `Name` instances from the same `Elements` are guaranteed
  comparable by `==` within one javac invocation.

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

### Dataflow expressions

- **PR #1643** — *Cache the hashCode for dataflow expressions.* Added
  a cached `hashCode` field across `ArrayAccess`, `ArrayCreation`,
  `BinaryOperation`, `FieldAccess`, `FormalParameter`, `LocalVariable`,
  `MethodCall`, `UnaryOperation`, and `ValueLiteral`. Per-object cost
  varies: `LocalVariable` pays zero because of the existing alignment
  gap; `FieldAccess` and similar pay +8 bytes. Peak overhead measured
  at ~128 bytes for a large method, well worth the savings on store-
  comparison hot paths.

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

### Correctness fixes adjacent to the perf work

These belong with the campaign because they were uncovered while
auditing the same files.

- **PR #1689** — *Preserve invariant `!isRunning => currentNode == null`
  even on exception.* Restores invariant in `AbstractAnalysis` finally
  block.
- **PR #1690** — *Change `catch Throwable` to `catch Exception`* in
  several framework call sites. `Throwable` accidentally suppressed
  things like `OutOfMemoryError` and `ThreadDeath`.

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

No candidates are currently open. Add new ones below as profiling surfaces
them, with the capture format above.

---

## Reproducing measurements

Use [`checker/bin-devel/record-jfr.sh`](../../checker/bin-devel/record-jfr.sh)
for trace capture; see
[`.claude/skills/cf-performance/SKILL.md`](../../.claude/skills/cf-performance/SKILL.md)
for the analysis pipeline and the known pitfalls (parallel-worker
constant-pool corruption, the silent 10 ms `MIN_SAMPLE_PERIOD` floor,
Maven multi-module filename handling). Always re-capture on the same
workload after applying a patch to confirm the targeted self-time
percentage moved. A patch that passes tests but doesn't move the
profile is wrong by definition.
