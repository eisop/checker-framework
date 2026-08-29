# Architectural Decision Log: Checker Framework as an Error Prone plugin

This document records the design decisions made while integrating the EISOP
Checker Framework (CF) so that it can run either as a standalone javac
annotation processor (the existing, canonical mode) or as an Error Prone (EP)
plugin. It is the raw material for the user-facing documentation produced in
Task 8. Entries are append-only and roughly chronological.

## Goals (as agreed with the maintainers)

- **1b (primary):** Let teams already using Error Prone enable CF type systems
  through their existing EP configuration.
- **1c (desirable):** Reuse EP's suggested-fix / patching pipeline for CF fixes.
- **Single entry point:** One EP check builds one AST/CFG and runs *multiple*
  selected CF type systems over it (reusing the CF's aggregate/subchecker
  infrastructure and shared-CFG support), rather than registering N independent
  annotation processors.
- **Diagnostics:** CF findings become EP `Description`s (option "2a"), so EP
  severity/suppression and the patch pipeline apply.
- **Keep standalone mode unchanged:** Core modules must not gain any EP or Guava
  dependency, and standalone annotation-processor behavior must not change.

## Fixed conventions

- New git branch: `cf-ep`.
- EP option namespace: `eisopcf` (e.g. `-XepOpt:eisopcf:checkers=...`). Chosen
  over `CheckerFramework` to keep EISOP CF distinct from the typetools CF.
- The new EP-specific Gradle module consumes a *published* `error_prone_check_api`
  artifact (latest release), never a composite build. See ADR-0002.

## Module layering (verified)

Current dependency order (no Guava/EP anywhere in these):

    checker-qual <- javacutil <- dataflow <- framework <- checker

- `SourceChecker`, `BaseTypeChecker`, `AggregateChecker` live in `framework`.
- Concrete checkers (e.g. `NullnessChecker`) live in `checker`.
- `AbstractTypeProcessor` lives in `javacutil`.

## Cyclic-dependency constraint (verified)

`error-prone/check_api` depends on `io.github.eisop:dataflow-errorprone`, which
is produced by *this* repo's `dataflow` module (`createDataflowShaded('errorprone')`
in `dataflow/build.gradle`). Therefore:

- The EP dependency must live only in the new leaf module(s).
- Core modules must never import `com.google.errorprone.*`.
- The new module consumes a *published* `error_prone_check_api` jar so the build
  graph edge crosses a published-artifact boundary (no Gradle project-path cycle).

`dataflow-errorprone` is package-relocated to
`org.checkerframework.errorprone.dataflow...`; the CF core uses the un-relocated
`org.checkerframework.dataflow`. The bridge must ensure the CF core loads its own
(un-relocated) dataflow classes, not EP's shaded copy. (Addressed in Task 3.)

---

## ADR-0001: Externally-driven mode for `AbstractTypeProcessor`

**Status:** accepted (Task 1)

**Context.** Both the CF and EP drive work from a `TaskListener` on
`TaskEvent.Kind.ANALYZE`-finished, walking a fully-attributed `TreePath`.
- CF: `AbstractTypeProcessor.AttributionTaskListener` -> `typeProcessingStart()`,
  then `typeProcess(TypeElement, TreePath)` per top-level class (the `TreePath`
  leaf is a `ClassTree`), then `typeProcessingOver()`.
- EP: `ErrorProneAnalyzer` (itself a `TaskListener`) invokes registered
  `BugChecker`s per tree node as it scans each attributed compilation unit.

In EP mode, EP already owns the `TaskListener` and already forces
`--should-stop=ifError=FLOW`. If the CF's `AbstractTypeProcessor.init()` also
registered its own `AttributionTaskListener`, type-checking would run twice
(once EP-driven, once CF-listener-driven).

**Decision.** Add an "externally-driven" mode to `AbstractTypeProcessor`:
- A protected boolean, default `false`, preserving the existing self-driven
  behavior for standalone mode.
- When externally driven, `init()` does **not** register the
  `AttributionTaskListener`. A host (the EP bridge) invokes the existing public
  `typeProcessingStart()` / `typeProcess(...)` / `typeProcessingOver()` methods
  itself, guarding the once-only lifecycle with the same flags the listener uses.
- The `shouldStopPolicy` bump to FLOW is kept unconditionally: it is idempotent
  and EP sets the same policy, so it is harmless in both modes.

**Consequences.**
- Standalone mode is byte-for-byte unchanged (the field defaults to self-driven).
- The core logic (`typeProcess` -> `visitor.visit`) is untouched and shared by
  both modes; no `com.google.errorprone.*` reference enters `javacutil` or
  `framework`.
- The host is responsible for the once-only `typeProcessingStart` /
  `typeProcessingOver` bracketing when externally driven; a public helper is
  exposed so the host does not duplicate the lifecycle guards.


### ADR-0001 notes: verification and API surface

- `AbstractTypeProcessor.setExternallyDriven(boolean)` is `protected`. Because the
  EP bridge lives in a different package and holds a `SourceChecker` reference,
  `SourceChecker` exposes a public `enableExternallyDrivenMode(boolean)` wrapper
  that must be called before `init(ProcessingEnvironment)`.
- Host-facing lifecycle helpers `typeProcessExternally(TypeElement, TreePath)` and
  `typeProcessingOverExternally()` handle the once-only `typeProcessingStart` /
  `typeProcessingOver` bracketing so the host does not duplicate the guard logic.
  They intentionally do **not** consult the `elements` set (populated only during
  the declaration annotation-processing round, which does not run meaningfully in
  EP mode); the host decides which classes to process and when it is done.
- Verification for Task 1 focuses on "standalone unchanged": `AggregateTest`,
  `CompoundCheckerTest`, and `ElementSuppressionTest` (which exercise the
  subchecker/aggregate lifecycle most directly) all pass unchanged. The
  externally-driven path itself is exercised end-to-end by the Error Prone
  `CompilationTestHelper` test added in Task 4, over a real compilation, rather
  than via a throwaway low-level `ProcessingEnvironment`/`TreePath` harness.
