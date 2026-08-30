# The Checker Framework as an Error Prone plugin (developer notes)

The EISOP Checker Framework can run as an Error Prone plugin (a single Error Prone check
named `eisopcf`) in addition to its standalone annotation-processor mode.

**User documentation** — how to enable the plugin, select type systems, pass options,
configure severity, suppress warnings, and use suggested fixes — is in the manual, in the
"Error Prone" section of the "Integration with external tools" chapter
(`docs/manual/external-tools.tex`). This file collects only the developer-facing details
not covered there. For the design rationale and the decisions behind the implementation,
see [`cf-errorprone-decisions.md`](cf-errorprone-decisions.md).

## Module layout

- **`framework-errorprone`** is the only module that depends on Error Prone. It is a
  JDK-21+-only leaf module (Error Prone requires JDK 21+), gated in `settings.gradle`, so
  it is absent from the build on older JDKs and standalone users are unaffected. It
  contains:
  - `EisopCheckerFrameworkPlugin` — the `@BugPattern("eisopcf")` `ClassTreeMatcher`
    registered with Error Prone via a hand-written
    `META-INF/services/com.google.errorprone.bugpatterns.BugChecker` resource.
  - `EisopContextAdapter` — obtains the javac `ProcessingEnvironment` from Error Prone's
    `VisitorState.context` (with `Context.get`, so that a context without one is diagnosed
    rather than silently given a fresh, unrelated environment), and reports which dataflow
    copy is loaded, so that a test can check that the Checker Framework's own (un-relocated)
    dataflow classes are used rather than Error Prone's shaded copy.
  - `CheckerFrameworkDriver` — instantiates the selected `SourceChecker`s by reflection and
    drives them per class through the externally-driven lifecycle. Contains no Error Prone
    types.
- The Checker Framework core modules (`checker-qual`, `javacutil`, `dataflow`, `framework`,
  `checker`) contain no Error Prone dependency. The plugin consumes a *published*
  `error_prone_check_api`, so there is no dependency cycle with the `dataflow-errorprone`
  artifact that Error Prone itself consumes.

## How a checker supplies a suggested fix

Fixes flow from a checker to Error Prone through a framework-agnostic channel, so the core
never references Error Prone. A checker attaches fixes to a finding by reporting a
`DiagMessage` that carries them:

```java
checker.report(tree, DiagMessage.error("nullness.on.primitive").withFixes(fixes));
```

Each fix is a `SuggestedFixData`, built from source positions with JDK types only (e.g.
`SuggestedFixData.deleteTree(sourcePositions, root, annotationTree)`); the plugin
translates it into an Error Prone `SuggestedFix`. See `NullnessNoInitVisitor` for a worked
example (removing a nullness annotation from a primitive type), and the decision log
(ADR-0007, ADR-0011) for the design.

## Testing and CI

The plugin's tests live in `framework-errorprone/src/test` and use Error Prone's
`CompilationTestHelper` / `BugCheckerRefactoringTestHelper`. They run automatically as part
of `./gradlew test` whenever the build JDK is 21 or newer (the module is absent on older
JDKs), so no separate CI job is required.

## Runnable example

`docs/examples/eisop-errorprone/` is a self-contained Gradle project that runs the Nullness
Checker as the `eisopcf` plugin. It consumes the locally-built Checker Framework jars (rather
than the published `io.github.eisop:framework-errorprone` artifact), so it exercises the
current checkout:

```
cd docs/examples/eisop-errorprone
make all
```

`make all` builds the required jars, compiles the demo, and checks that the expected
`[eisopcf] [dereference.of.nullable]` diagnostic is produced (JDK 21+; a no-op otherwise).
Its `build.gradle` doubles as a template for a real build.
