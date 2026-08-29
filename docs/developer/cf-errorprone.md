# Running the EISOP Checker Framework as an Error Prone plugin

The EISOP Checker Framework can run in two ways:

1. **Standalone** — as a javac annotation processor (`-processor ...`). This is the
   canonical mode and is unchanged.
2. **As an Error Prone plugin** — one Error Prone check, named `eisopcf`, runs one or
   more Checker Framework type systems during an Error Prone compilation.

This document describes mode 2. For the design rationale and the decisions behind it,
see [`cf-errorprone-decisions.md`](cf-errorprone-decisions.md).

## Why run as an Error Prone plugin?

- Teams already using Error Prone can enable Checker Framework type systems through
  their existing Error Prone configuration, in the same compilation as their other
  Error Prone checks.
- Checker Framework findings become Error Prone diagnostics, so they honor Error
  Prone's severity configuration, its suppression mechanism, and its suggested-fix /
  patch pipeline.

Standalone mode remains fully supported and is unaffected. Choose whichever fits your
build.

## Requirements

- **JDK 21 or later.** Error Prone itself requires JDK 21+, so the plugin (the
  `framework-errorprone` artifact) is a JDK-21+-only module.
- Error Prone on the compile path, configured as usual (for example, via the
  `net.ltgt.errorprone` Gradle plugin).

## Dependencies

Put the plugin and Error Prone on the annotation processor / Error Prone path, and put
the Checker Framework checkers you want to run on the same path so the plugin can load
them by name.

Conceptually (coordinates are illustrative; the `framework-errorprone` artifact is
published as part of a Checker Framework release):

```
errorprone("com.google.errorprone:error_prone_core:<version>")
errorprone("io.github.eisop:framework-errorprone:<version>")   // the eisopcf plugin
errorprone("io.github.eisop:checker:<version>")                // the checkers to run
```

The plugin resolves each selected checker class by name at run time, so any Checker
Framework checker on the path can be selected.

## Selecting type systems

Choose the type system(s) to run with the `eisopcf:checkers` Error Prone option: a
comma-separated list of fully-qualified `SourceChecker` class names.

```
-XepOpt:eisopcf:checkers=org.checkerframework.checker.nullness.NullnessChecker
```

Multiple type systems run together over a single compilation (one parsed and attributed
AST):

```
-XepOpt:eisopcf:checkers=org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker
```

Each selected type system builds its own control-flow graph, exactly as standalone
`javac -processor A,B` does. (See the decision log, ADR-0006, for why a single shared
CFG across independent type systems is not provided.)

## Required javac options

Error Prone imposes these javac options; the Checker Framework plugin needs them too:

```
-XDcompilePolicy=simple
--should-stop=ifError=FLOW
-XDaddTypeAnnotationsToSymbol=true
```

Because the Checker Framework reaches into javac internals, the compiler (and, when
running Error Prone in-process, the host JVM) needs these exports/opens:

```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-opens   jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
```

## Severity

`eisopcf` is one Error Prone check, so its severity is configured per check, not per
finding. It defaults to `WARNING`. Override it with standard Error Prone configuration:

```
-Xep:eisopcf:ERROR
-Xep:eisopcf:OFF
```

Because severity is per check, all findings from all selected type systems share the
`eisopcf` severity. The Checker Framework's own error/warning distinction is preserved
textually: warning-level findings are prefixed with `[warning]` in the message.

## Suppression

Suppress `eisopcf` findings with Error Prone's mechanism:

```java
@SuppressWarnings("eisopcf")
```

on the finding's enclosing element (for example, the method). Finer-grained Checker
Framework suppression keys (e.g. `@SuppressWarnings("nullness")`) continue to work
through the Checker Framework's own suppression handling, which runs before a finding
is reported to Error Prone.

## Suggested fixes and patching

Every `eisopcf` finding carries an "add `@SuppressWarnings("eisopcf")`" suggested fix,
just as Error Prone's own checks do. Running Error Prone in patch mode inserts the
suppression on the enclosing element. This makes the Checker Framework participate in
Error Prone's suggested-fix / patch workflow.

(The Checker Framework does not yet produce other machine-applicable fixes for its
findings; when it does, they will flow through to Error Prone's patch pipeline
automatically. See the decision log, ADR-0007.)

## Relationship to standalone mode

- Standalone annotation-processor mode is unchanged and still supported on JDK 8+.
- The `framework-errorprone` module is the only place that depends on Error Prone; the
  Checker Framework core modules (`checker-qual`, `javacutil`, `dataflow`, `framework`,
  `checker`) do not depend on Error Prone, so standalone users are unaffected.

## Continuous integration

The plugin's tests run automatically as part of the Checker Framework's JUnit test
suite (`./gradlew test`) whenever the build JDK is 21 or newer; on older JDKs the module
is not part of the build. No separate CI job is required.
