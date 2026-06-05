# Notes for AI assistants working on the EISOP Checker Framework

This file is read automatically by Claude Code and can be used by other AI
coding assistants as orientation when working on this repository. It does
not replace [`CONTRIBUTING.md`](CONTRIBUTING.md) or the
[Developer Manual](https://htmlpreview.github.io/?https://github.com/eisop/checker-framework/master/docs/developer/developer-manual.html);
it consolidates the project-specific conventions that come up most often
in assistant interactions and the pitfalls that have been learned the hard
way.

## Project in one paragraph

The EISOP Checker Framework is a Java annotation processor that implements
pluggable type systems on top of `javac`. The framework itself (in
`framework/`) provides the visitor + dataflow + qualifier-hierarchy
infrastructure; individual checkers (in `checker/`) define qualifier lattices
and implement the type rules. Hot paths run inside forked `javac` invocations
during `./gradlew alltests` and during user builds.

## Repository topology

The Gradle subprojects matter; in rough dependency order:

- `checker-qual/` — public qualifier annotations only. No runtime
  dependency on the framework. **Public API surface; touch with care.**
- `javacutil/` — low-level utilities over `javac` internals
  (`ElementUtils`, `TreeUtils`, `AnnotationUtils`, `TypesUtils`,
  `BugInCF`, `UserError`, etc.). Called from everywhere.
- `dataflow/` — the dataflow framework (`CFAbstractAnalysis`,
  `CFAbstractTransfer`, `CFAbstractStore`, `CFAbstractValue`).
- `framework/` — the bulk of the type-checking infrastructure:
  - `framework/type/` — `AnnotatedTypeMirror`, `AnnotatedTypeFactory`,
    `GenericAnnotatedTypeFactory`, type annotators, type validators.
  - `framework/util/` — defaults, dependent-types, qualifier polymorphism.
  - `framework/source/` — the `SourceChecker`/`SourceVisitor` entry point.
  - `common/basetype/` — `BaseTypeVisitor`, `BaseTypeChecker`,
    `BaseTypeValidator`, `BaseAnnotatedTypeFactory`.
  - `common/value/`, `common/wholeprograminference/`,
    `common/returnsreceiver/`, ... — shared checker support.
- `framework-test/` — JUnit harness for checker test inputs.
- `checker/` — individual checkers (`nullness`, `initialization`,
  `index`, `signature`, `regex`, `formatter`, `mustcall`,
  `calledmethods`, `resourceleak`, `optional`, `tainting`,
  `units`, `interning`, `lock`, `i18n`, `i18nformatter`, `fenum`,
  `signedness`, `purity`, ...).
- `checker-util/` — shared checker utilities.

## Building and testing

```
./gradlew assemble                       # build only
./gradlew assembleForJavac               # build to use checker/bin/javac
./gradlew alltests                       # full test suite (long)
./gradlew test                           # framework + javacutil + dataflow JUnit
./gradlew :checker:test                  # all checker JUnit tests
./gradlew :checker:test --tests "<class>"  # one test class
./checker/bin-devel/test-cftests-junit.sh  # what CI runs
ORG_GRADLE_PROJECT_useJdkVersion=21 ./gradlew test  # cross-JDK
```

CI scripts live in [`checker/bin-devel/test-*.sh`](checker/bin-devel/);
reproducing a CI failure locally usually means running the matching
script with the same `useJdkVersion`. CI workflow is
[`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Commit and PR conventions

- **One logical change per commit.** Patches that bundle perf + style +
  refactor get rejected.
- **Subject line:** imperative mood, package/file scope first, then the
  change. Examples taken from real history:
  - `Avoid Integer boxing and lambda for ATM hashCode`
  - `Clear SubtypeVisitHistory to avoid huge caches that slow down checking`
  - `Cache the hashCode for dataflow expressions`
  - `Increase default cache size from 300 to 2000`
- **Body:** problem then fix. If JFR data motivates the change, cite the
  approximate self-time percentage and the workload. No marketing
  language ("dramatically", "blazingly").
- **Changelog:** every user-visible or perf-relevant change gets one
  bullet in [`docs/CHANGELOG.md`](docs/CHANGELOG.md) under the next
  release.
- **Branch naming for perf/correctness audits:**
  `review-<package>` or `perf-<package>` matches recent practice
  (e.g., `Review of common/basetype package (#1721)`).
- **`git format-patch` output is welcome** for proposing series; the
  maintainer often prefers `git am`-ready patches over GitHub PRs for
  large mechanical sweeps.

## How to propose performance work

The full procedure is in [`.claude/skills/cf-performance/SKILL.md`](.claude/skills/cf-performance/SKILL.md).
Read that skill before proposing any perf change. The short version:

1. Profile first (JFR; see `checker/bin-devel/record-jfr.sh`).
2. Identify hotspots from real samples, not from reading code.
3. Check [`docs/developer/performance-notes.md`](docs/developer/performance-notes.md)
   to avoid re-proposing optimizations that have already been applied
   or that have been tried and rejected.
4. Produce a minimal patch with a JFR-backed commit message.
5. Run `./gradlew alltests` before submitting — perf patches frequently
   break subtle semantics (the `AnnotationMirrorSet#addAll` two-path
   bug is the canonical example).

## Things to be careful about

- **`checker-qual/` is public API.** Any signature change there breaks
  downstream users.
- **`AnnotatedTypeMirror` invariants** — interning, structural equality,
  `hashCode`/`equals` contract — are subtle and rely on visitor reset
  behavior. Test thoroughly when modifying type-related caches.
- **`final` fields that are visitor state** sometimes need to be
  reassignable (`AnnotatedTypeScanner.visitedNodes`). Audit subclass
  references before changing.
- **Identity-keyed caches** on `Element`/`Tree` should use
  `IdentityHashMap`, not `HashMap`. The session-unique nature of these
  keys is documented in the JDK API.
- **`Name` vs. `String` comparison:** `Name.contentEquals(CharSequence)`
  decodes the underlying UTF-8 buffer; for hot paths, compare interned
  `String`s instead. See `AnnotationUtils.annotationName`.

## How AI assistants are expected to work here

- **Default to producing patches, not explanations.** If a change is
  obvious, write the diff. Save the prose for the commit message.
- **Don't re-propose already-applied optimizations.** Cross-check
  [`docs/developer/performance-notes.md`](docs/developer/performance-notes.md)
  and recent commits (`git log --grep=cache --grep=hashCode --grep=allocation`)
  before suggesting hot-path changes.
- **Verify before claiming.** Past examples of wrong claims that wasted
  a review cycle: "iterator() wraps unmodifiably" (the wrapper comes
  from `makeUnmodifiable()`, not `iterator()`); "`copyMap` saves
  allocations" (it doesn't, in the relevant call site); proposing
  `getEffectiveAnnotations()` as a hotspot (it was 0.05% of samples).
  If the reasoning is shaky, label it as a hypothesis, not a finding.
- **`git format-patch` output is preferred** for non-trivial work, with
  each patch standalone and committable.

## Pointers to deeper docs

- [`docs/manual/`](docs/manual/) contains the user manual, including
  descriptions for all type systems. In particular
  [`docs/manual/creating-a-checker.tex`](docs/manual/creating-a-checker.tex)
  contains information on how to create a new type system; it gives a
  high-level overview of how the EISOP Checker Framework works.
- [`docs/developer/developer-manual.html`](docs/developer/developer-manual.html)
  — long-form developer manual.
- [`docs/CHANGELOG.md`](docs/CHANGELOG.md) — release notes; the canonical
  log of behavioral changes.
- [`docs/developer/performance-notes.md`](docs/developer/performance-notes.md)
  — what's been profiled, optimized, tried-and-rejected.
- [`.claude/skills/`](.claude/skills/) — task-specific skills for
  performance work and patch authoring.
