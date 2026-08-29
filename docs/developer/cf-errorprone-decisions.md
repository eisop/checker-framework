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


---

## ADR-0003: Context -> ProcessingEnvironment via JavacProcessingEnvironment.instance

**Status:** accepted (Task 3)

**Context.** `SourceChecker.init(ProcessingEnvironment)` needs a real
`ProcessingEnvironment` (for `Trees`, `Messager`, `Elements`, `Types`, options).
In Error Prone mode the bridge only has Error Prone's `VisitorState.context`, a
`com.sun.tools.javac.util.Context`.

**Decision.** Obtain the environment with
`JavacProcessingEnvironment.instance(context)`. This works because the
`JavacProcessingEnvironment` singleton stays registered in the `Context` through
the ANALYZE phase (when Error Prone, and therefore this plugin, runs). It is
precedented: NullAway does the same (`Trees.instance(JavacProcessingEnvironment
.instance(state.context))`). `SourceChecker.unwrapProcessingEnvironment`
explicitly recognizes a `JavacProcessingEnvironment` and uses it as-is, so this is
*the same* environment the CF gets in standalone mode.

The adapter (`EisopContextAdapter`) contains no Error Prone types — only the JDK
compiler API — so the Context-to-ProcessingEnvironment concern is unit-testable
without constructing a `VisitorState`.

**Consequences / caveats.**
- Annotation processing must be enabled: with `-proc:none` no
  `JavacProcessingEnvironment` is created. The adapter throws a clear
  `IllegalStateException` in that case. (Error Prone itself runs with processing
  enabled, so this is not a practical restriction.)
- Element/type resolution via the environment is only valid *while the compiler is
  live* (during the ANALYZE callback), not after the task completes. The Error
  Prone plugin runs mid-compilation, so this is naturally satisfied; the unit test
  runs its assertions inside an ANALYZE `TaskListener` for the same reason.

## ADR-0003 notes: dataflow classloader guard (Task 3)

**Concern (from ADR-0002 notes).** Error Prone's classpath carries a
package-relocated, older dataflow copy (`org.checkerframework.errorprone.dataflow`,
3.41.x), while the CF core uses the un-relocated `org.checkerframework.dataflow`
(3.49.x). A CF checker running under Error Prone must use the un-relocated copy.

**Finding.** The two copies have *different package names*, so they coexist as
distinct classes with no collision. `Class.forName("org.checkerframework.dataflow
.cfg.ControlFlowGraph")` resolves to the CF's own un-relocated class; the relocated
FQN (`org.checkerframework.errorprone.dataflow...`) is a different class entirely.
`EisopContextAdapter.loadedDataflowPackage()` exposes which is loaded, and the test
`EisopContextAdapterTest` asserts (a) the un-relocated package is what resolves and
(b) both copies are present yet distinct. No shading/relocation work is needed on
our side; the guard is a regression test rather than a code fix.

**Significance.** This was the highest-risk unknown in the plan (the
Context->ProcessingEnvironment bridge and shaded-dataflow coexistence). Both work
with a thin adapter and no invasive changes, which de-risks Tasks 4-7.


---

## ADR-0004: Umbrella BugChecker drives the CF via reflection (Task 4)

**Status:** accepted (Task 4)

**Design.** `EisopCheckerFrameworkPlugin` (the `eisopcf` `ClassTreeMatcher`) reads
the comma-separated `-XepOpt:eisopcf:checkers=<FQN>[,<...>]` option through an
`@Inject EisopCheckerFrameworkPlugin(ErrorProneFlags)` constructor
(`flags.getListOrEmpty("eisopcf:checkers")`). On the first `matchClass` of a
compilation it builds a `CheckerFrameworkDriver`, which:
1. obtains the `ProcessingEnvironment` from the `VisitorState.context` via
   `EisopContextAdapter` (ADR-0003);
2. instantiates each selected `SourceChecker` reflectively by name (no compile-time
   dependency on `:checker`);
3. calls `enableExternallyDrivenMode(true)` then `init(procEnv)` (ADR-0001);
4. per class, calls `typeProcessExternally(classSymbol, state.getPath())`.

The class symbol comes from `ASTHelpers.getSymbol(ClassTree)` and the `TreePath`
from `VisitorState.getPath()` (leaf = the matched `ClassTree`).

**End-of-compilation.** The CF's `typeProcessingOver` is a whole-compilation step
(e.g. unneeded-suppression warnings), but `ClassTreeMatcher` has no end hook. The
driver registers a `MultiTaskListener.instance(context).add(listener)` that calls
`CheckerFrameworkDriver.finish()` on the `TaskEvent.Kind.COMPILATION`-finished
event. (`MultiTaskListener` is the Context-level way to add a `TaskListener`;
`JavacTask.instance` takes a `ProcessingEnvironment`, not a `Context`.)

**Diagnostics (2b).** `matchClass` returns `Description.NO_MATCH`; the CF reports
through its own `Messager`. Task 5 will switch to emitting EP `Description`s.

**Findings worth remembering.**
- **Checker jar variant.** A plain `testImplementation project(':checker')`
  resolves to `:checker`'s skinny/runtime variant, which did NOT put
  `NullnessChecker` on the test classpath (confirmed with a probe: plain
  `Class.forName` failed, unrelated to Error Prone). The fix is to depend on the
  shadow ("-all") jar file directly:
  `testImplementation files(tasks.getByPath(':checker:shadowJar').archiveFile) { builtBy ':checker:shadowJar' }`.
  A `project(path: ':checker', configuration: 'shadowRuntimeElements')` dependency
  fails variant matching because the shadow variant is tagged Java-8 while this
  module is Java-21. The fat jar is also what users really put on the processorpath,
  so this is deployment-faithful.
- **Classloader resolution.** `CheckerFrameworkDriver.loadCheckerClass` tries, in
  order, the classloaders of `SourceChecker`, the thread context, and the driver
  itself. In the test all three are the same app classloader; the multi-loader
  approach is defensive for deployments where Error Prone loads the plugin through a
  `MaskedClassLoader` from the processorpath.
- **Error Prone required javac exports for in-process tests.** The
  `CompilationTestHelper` runs javac in the test JVM; Error Prone's `ASTHelpers`
  triggers a superclass access check on `com.sun.tools.javac.parser.JavaTokenizer`,
  so `--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED` must be a
  Test JVM arg (in addition to the other javac-internal exports). These are JVM
  args, not compiler args (passing `--add-opens`/`--add-exports` as compiler args
  yields "has no effect at compile time" warnings).
- **Test source packages.** Put test sources in a named package to avoid Error
  Prone's own `DefaultPackage` check firing on the sample code and colliding with
  the `// BUG:` marker lines.


---

## ADR-0005: DiagnosticSink seam maps CF findings to Error Prone Descriptions (Task 5)

**Status:** accepted (Task 5)

**Context.** For "2a", Checker Framework findings should become Error Prone
`Description`s so Error Prone severity, `@SuppressWarnings("eisopcf")` suppression,
and (later) the patch pipeline apply. The core (`framework`) must not reference any
Error Prone type.

**Seam.** All CF diagnostics funnel through a single method:
`SourceChecker.printOrStoreMessage(kind, message, source, root, trace)` — both the
direct path (no subcheckers) and the buffered aggregate/subchecker path (which
flushes via `printStoredMessages` -> the same method on the parent). This is the
one choke point to intercept, and intercepting on the parent covers multi-checker
runs (Task 6) as well.

**Decision.**
- Add `org.checkerframework.framework.source.DiagnosticSink`, a `@FunctionalInterface`
  with `report(Diagnostic.Kind, String message, Tree source, CompilationUnitTree root)`.
  It uses only `javax.tools` and `com.sun.source.tree` types — no Error Prone, no
  host types — so the core stays framework-agnostic.
- `SourceChecker` gets a nullable `diagnosticSink` field and `setDiagnosticSink(...)`.
  When set, the 5-arg `printOrStoreMessage` calls the sink instead of
  `Trees.printMessage`. Default null => unchanged standalone behavior (verified:
  AggregateTest/CompoundCheckerTest/ElementSuppressionTest still pass).
- In `framework-errorprone`, `CheckerFrameworkDriver.create(context, names, sink)`
  installs the sink on every checker. `EisopCheckerFrameworkPlugin.diagnosticSink()`
  builds a sink that, using the `VisitorState` active during `matchClass` (stored in
  a transient `currentState` field for the duration of the call), does
  `state.reportMatch(buildDescription(sourceTree).setMessage(msg).build())`.

**Severity / suppression semantics (documented limitations).**
- Error Prone severity is per-*check*, not per-finding. All `eisopcf` findings share
  the `eisopcf` severity (default WARNING; override with `-Xep:eisopcf:ERROR`). The
  CF diagnostic kind is preserved textually: warnings get a `[warning]` message
  prefix. (A future refinement could split into separate checks per severity.)
- `@SuppressWarnings("eisopcf")` is honored at the granularity of the enclosing
  class (the tree the plugin matches, which is the `VisitorState` path when the sink
  fires). Finer-grained CF `@SuppressWarnings` keys still work via the CF's own
  suppression, which runs before a finding ever reaches the sink.

**Verified (EisopCheckerFrameworkPluginTest).** return-null finding appears as an
`eisopcf` diagnostic; `@SuppressWarnings("eisopcf")` suppresses it (proving it is an
EP `Description`, not Messager output); `-Xep:eisopcf:ERROR` is accepted.


---

## ADR-0006: Multiple type systems share the AST, not the CFG (Task 6)

**Status:** accepted (Task 6)

**Goal.** `-XepOpt:eisopcf:checkers=A,B[,...]` runs several Checker Framework type
systems together in one Error Prone / javac invocation.

**Investigation of "shared CFG."** The plan wording mentioned running type systems
over "one shared AST/CFG." A true shared *CFG object* across independent type
systems is not something the Checker Framework supports, and the maintainers chose
not to pursue it (option "a"):
- `AggregateChecker` explicitly performs no sharing ("no communication, interaction,
  or cooperation between the component checkers").
- `GenericAnnotatedTypeFactory.getSharedCFGForTree` / `addSharedCFGForTree` key the
  shared CFG on the *ultimate parent* `BaseTypeChecker` and are designed for
  genuinely cooperating subcheckers of one checker (e.g. Nullness + Initialization,
  which share a qualifier hierarchy). `getUltimateParentChecker()` only walks up
  through `BaseTypeChecker` parents, so subcheckers under an `AggregateChecker`
  (a `SourceChecker`, not a `BaseTypeChecker`) are each their own CFG root.
- Even standalone CF (`javac -processor A,B`) does **not** share a CFG across
  independently-listed checkers: each `AbstractTypeProcessor` builds its own.
- Forcing unrelated type systems to be subcheckers of a synthetic `BaseTypeChecker`
  parent (to reuse the shared-CFG cache) would impose a qualifier-hierarchy/factory
  relationship they are not designed for, with real correctness risk, and would
  diverge from how the CF actually composes checkers.

**Decision.** Adopt "shared AST, per-checker CFG": one javac / Error Prone
invocation over one parsed-and-attributed AST, with each selected type system
building its own CFG — exactly as standalone `javac -processor A,B` behaves. This is
what the existing driver already does (it instantiates the selected `SourceChecker`s
and drives each per class over the same `Context`). No new composition model is
introduced.

**Implementation.** No core (`framework`) change was needed; Task 5's `DiagnosticSink`
already covers multiple checkers (each reports through the sink). The plugin change
is limited to surfacing a configuration error (e.g. an unresolvable checker name)
once, as a clean `eisopcf` diagnostic on the class, instead of an unhandled plugin
exception (`configErrorReported` flag; `matchClass` catches `IllegalArgumentException`
from driver creation).

**Verified (EisopCheckerFrameworkPluginTest).**
- `multipleCheckersRunTogether`: Nullness (`return.type.incompatible`) and Interning
  (`not.interned`) findings both appear from one compilation with a comma-separated
  `eisopcf:checkers` list.
- `unknownCheckerNameIsReported`: a bogus checker name yields a clear "Checker class
  not found" `eisopcf` diagnostic.


---

## ADR-0007: Fix seam + suppression fix through Error Prone's patch pipeline (Task 7)

**Status:** accepted (Task 7)

**Finding: the Checker Framework has no per-diagnostic suggested-fix mechanism.**
CF diagnostics (`CheckerMessage`, `printOrStoreMessage`, the `message(...)` chain)
carry only `(kind, message, source tree, trace)` — no attached machine-applicable
edit, unlike Error Prone's `SuggestedFix`. The closest capability is whole-program
inference (`-Ainfer`), a batch/offline pass that writes `.jaif`/`.ajava`/stub files,
not per-finding fixes on the diagnostic path. Building general fix generation into
the CF is a large, separate feature out of scope here.

**Decision (agreed with maintainers, option a).** Two parts:

1. **Plumb a neutral fix channel** so fixes reach a host's patch pipeline the moment
   any checker produces them:
   - `SuggestedFixData` (new, in `framework`): a list of `Replacement(startPosition,
     endPosition, text)` using javac source offsets — JDK-neutral, no Error Prone
     type. The core stays framework-agnostic.
   - `DiagnosticSink` gains a `default reportWithFix(kind, message, source, root,
     @Nullable SuggestedFixData)` that by default ignores the fix and delegates to
     `report(...)`, so existing lambda sinks keep working. No CF code calls it with a
     non-null fix yet (the CF produces none), but the channel is defined and unit
     -tested (`SuggestedFixDataTest`), and the module translates it to an Error Prone
     `SuggestedFix` (`EisopCheckerFrameworkPlugin.toErrorProneFix`, position-based
     `SuggestedFix.Builder.replace`).

2. **A real, always-available suppression fix** (mirrors Error Prone's own checks):
   every `eisopcf` finding's `Description` carries
   `SuggestedFixes.addSuppressWarnings(state, "eisopcf")`. Applying it via Error
   Prone's refactoring/patch pipeline inserts `@SuppressWarnings("eisopcf")`. This is
   what proves goal 1c end-to-end.

**Important refinement: anchor findings at their own source tree.** The sink now
computes the finding's `TreePath` (`TreePath.getPath(root, source)`) and uses
`state.withPath(...)`, so both the reported position and the suppression fix anchor
at the finding's location (e.g. the enclosing method), not the class that
`matchClass` is visiting. Before this, the suppression landed on the class. This
also makes reported diagnostics point at the finding rather than the class.

**Verified.**
- `EisopCheckerFrameworkPatchTest.suppressionFixIsApplied`: Error Prone's
  `BugCheckerRefactoringTestHelper` applies the fix, inserting
  `@SuppressWarnings("eisopcf")` on the enclosing method.
- `SuggestedFixDataTest`: the neutral fix representation (factory, ordering,
  immutability).
- Standalone CF unaffected (`AggregateTest`, `CompoundCheckerTest` pass); existing
  plugin tests (including class-level suppression) still pass.

**Follow-up (out of scope).** When the CF gains per-finding fixes, route them through
`reportWithFix` with `SuggestedFixData`; the module already translates and attaches
them, so no further core or module change is required.


---

## ADR-0008: Documentation, runnable example, and CI (Task 8)

**Status:** accepted (Task 8)

**Documentation.** Added `docs/developer/cf-errorprone.md`, a user-facing guide (why
to use the plugin, JDK 21+ requirement, dependencies, `-XepOpt:eisopcf:checkers`,
required javac options and `--add-exports`/`--add-opens`, severity, suppression, and
the suppression suggested-fix / patch workflow). It complements this decision log. The
LaTeX manual was intentionally left untouched while the feature is on a branch; it can
reference the guide once merged.

**Runnable example.** Added `docs/examples/eisop-errorprone/`, a standalone Gradle
project (its own empty `settings.gradle`, mirroring the sibling `errorprone` example)
that runs the Nullness Checker as the `eisopcf` plugin over a demo class with a
nullness bug. Because `framework-errorprone` is not published yet, the example consumes
the *locally-built* jars by file:
- `checker-qual-<v>.jar` (compileOnly, for `@Nullable`);
- the plain (non-shadow) `framework-errorprone-<v>.jar` on the `errorprone` path — the
  `-all` shadow jar must NOT be used, as it bundles a copy of Error Prone's classes and
  breaks the `BugChecker` service check with "not a subtype";
- the `checker-<v>-all.jar` shadow jar (bundles the checkers) on the `errorprone` path.
A `Makefile` builds those jars (`:checker-qual:jar :framework-errorprone:jar
:checker:shadowJar`), runs the example, and greps for the expected
`[eisopcf] [dereference.of.nullable]` diagnostic. It is gated on JDK 21+ (no-op below).
`.gitignore` gained `docs/examples/eisop-errorprone/{.gradle/,Out.txt}` entries, matching
the other examples. Verified: `make all` exits 0.

**CI.** No workflow change is needed. `./gradlew test` (run by the existing
`cftests-junit` job, whose primary JDK is 21) includes `:framework-errorprone:test`
automatically when the module is present, and `settings.gradle` excludes the module on
JDK <= 17. Confirmed with `gradlew test --dry-run`.
