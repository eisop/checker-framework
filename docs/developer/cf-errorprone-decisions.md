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


---

## ADR-0002: `framework-errorprone` is a JDK-21+-only leaf module

**Status:** accepted (Task 2)

**Context.** The new EP bridge module must compile and run against Error Prone
`error_prone_check_api` 2.50.0. Error Prone requires JDK 21+ (its own build uses
a JDK-21 toolchain; EP as a tool does not run below 21). Every existing CF module,
however, is compiled with `sourceCompatibility = targetCompatibility = 8` so that
the Checker Framework can run under Java 8. These two requirements cannot both hold
for the bridge module.

The repo already gates Error Prone itself on `useJdkVersionInt >= 21` (see the root
`build.gradle` `dependencies` block that only adds `error_prone_core` when the build
JDK is 21+).

**Decision.**
- `framework-errorprone` is included in the Gradle build **only** when the build JDK
  is 21+ (`useJdkVersionInt >= 21`), via a conditional `include` in `settings.gradle`
  (chosen option "a"). On Java 8/11/17 builds the module is not part of the build at
  all, so it never configures its EP dependency and cannot affect the standalone
  Java 8 build path.
- Inside the module, override the global Java 8 compatibility: compile with
  `options.release = 21` and the same `--add-exports jdk.compiler/...` flags the rest
  of the build uses on JDK 9+ (the CF reaches into `com.sun.tools.javac.*`).
- Consequence, stated plainly: **CF-as-an-Error-Prone-plugin requires JDK 21+.**
  Standalone annotation-processor mode keeps its full Java 8+ support, unchanged.
  No capability is lost, since EP already requires 21+.

**Version.** Reuse the existing central `versions.errorprone` property (`2.50.0`),
which is already the latest published `error_prone_check_api` release and the version
the CF is otherwise built/tested against. No new version property is introduced.

**Consequences / follow-ups.**
- Release scripts will later need updating so the JDK-21+-only artifact is built and
  published correctly. Deferred (agreed with maintainers) — not addressed in this task.
- The module gets the shared root config automatically (spotless, the Error Prone
  build-time linter with `-Werror`, publishing scaffolding), so its own code must be
  EP-linter-clean.


### ADR-0002 notes: BugChecker service registration without @AutoService

**Problem.** The obvious way to register the Error Prone plugin is
`@AutoService(BugChecker.class)`, which relies on the auto-service annotation
processor to generate `META-INF/services/com.google.errorprone.bugpatterns.BugChecker`.
In this repo that processor never runs reliably for a subproject:

- The root `build.gradle` reassigns `options.annotationProcessorPath =
  configurations.errorProneAnnotationProcessor` in a subprojects `afterEvaluate`,
  and the `net.ltgt.errorprone` plugin (v5.1.1) runs Error Prone as a compiler
  `-Xplugin` rather than a plain annotation processor.
- Even after forcing auto-service onto the processor path, javac reported
  "No processor claimed ... @AutoService, @BugPattern": `error_prone_core` drags
  in conflicting transitive `auto-service` / `auto-common` versions, so the
  auto-service processor did not claim the annotation.

**Decision.** Do not use `@AutoService`. Register the plugin with a hand-written
resource file `src/main/resources/META-INF/services/com.google.errorprone.bugpatterns.BugChecker`
containing the fully-qualified plugin class name. This is deterministic, needs no
annotation processor, and is exactly what Error Prone's `ServiceLoader`-based
`ErrorPronePlugins` discovery reads. It also keeps the module's annotation
processor path untouched, avoiding the version-conflict fragility above.


### ADR-0002 notes: verified dependency facts (Task 2)

- **No cycle.** `framework-errorprone` -> published `error_prone_check_api:2.50.0`
  -> published `io.github.eisop:dataflow-errorprone:3.41.0-eisop1`. That last
  artifact is a *released* jar, distinct from this reactor's `:dataflow` project
  (currently 3.49.x). The version skew across the published-artifact boundary is
  precisely what keeps Gradle from seeing a project-path cycle.
- **Core stays EP-framework-free.** `:javacutil`, `:dataflow`, `:framework`
  runtime classpaths contain no `error_prone_check_api`, `error_prone_core`, or
  real `com.google.guava:guava`. They do reference `error_prone_annotations` (a
  trivial annotations-only jar) and `org.checkerframework.annotatedlib:guava`
  (the CF's annotated Guava stubs) — both pre-existing and unrelated to the EP
  framework. The requirement is "no EP *framework* in core," which holds.
- **Watch for Task 3:** EP bundles `dataflow-errorprone` *relocated* to
  `org.checkerframework.errorprone.dataflow...`, and at an *older* version than
  this repo's `:dataflow`. When the bridge runs a CF checker, the checker must use
  the CF core's own un-relocated `org.checkerframework.dataflow` (from `:framework`
  -> `:dataflow`), not EP's shaded/older copy. Package names differ, so they can
  coexist, but this must be verified in Task 3.

### Build wiring facts (Task 2)

- `settings.gradle` computes the build JDK major version directly (settings is
  evaluated before build.gradle) and only `include 'framework-errorprone'` when it
  is >= 21.
- The module sets `sourceCompatibility`/`targetCompatibility = 21` (NOT
  `options.release`, which is incompatible with `--add-exports` of JDK system
  packages) and passes the same `--add-exports jdk.compiler/...` the rest of the
  build uses on JDK 9+.
- The module inherits the shared root config (spotless; the `net.ltgt.errorprone`
  build-time linter with `-Werror`). Its code must therefore be EP-linter-clean;
  `@SuppressWarnings("BugPatternNaming")` is used on the plugin class because the
  canonical check name (`eisopcf`) intentionally differs from the class name.
