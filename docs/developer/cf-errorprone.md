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

## Checker Framework options

Pass Checker Framework options **exactly as in standalone mode: with javac `-A`
options**. They reach the checkers unchanged, because the plugin hands each checker the
real `javac` processing environment, whose options map javac populates from `-A` on the
command line. (Error Prone runs with annotation processing enabled, so javac records
`-A` options even though no Checker Framework annotation processor is registered. You do
*not* use `-XepOpt:` for Checker Framework options — `-XepOpt:eisopcf:...` is only for the
plugin's own options, currently just `checkers`.)

This covers both kinds of Checker Framework option:

- **Common options** (defined on `SourceChecker`, shared by all checkers), for example:

  ```
  -Astubs=/path/to/my.astub
  -AsuppressWarnings=nullness
  -Alint=cast:unsafe
  ```

- **Checker-specific options**, written `-ACheckerName_option=value` (note the `_`
  separator between the checker's simple name and the option), for example:

  ```
  -ANullnessChecker_someOption=value
  ```

For example, adding `-AsuppressWarnings=nullness` to a compilation running the Nullness
Checker under `eisopcf` suppresses all `nullness`-prefixed findings, just as it would in
standalone mode.

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

There are two suppression layers, giving you both per-type-system and coarse control.

### Per-type-system (fine-grained), at any granularity

Use the Checker Framework's own suppression keys. These work at any granularity — a
local variable, method, class, etc. — because the Checker Framework applies its
suppression *before* a finding is turned into an `eisopcf` Error Prone diagnostic, so a
suppressed finding never reaches Error Prone:

```java
@SuppressWarnings("nullness")     // suppresses only Nullness Checker findings
@SuppressWarnings("interning")    // suppresses only Interning Checker findings
@SuppressWarnings("allcheckers")  // suppresses all Checker Framework findings
@SuppressWarnings("nullness:dereference.of.nullable")  // a specific message key
```

So even though several type systems run under one `eisopcf` Error Prone check, you can
still suppress them independently and per method. **This is the recommended way to
suppress specific findings.**

### The Error Prone `eisopcf` key (any declaration)

```java
@SuppressWarnings("eisopcf")
```

suppresses *all* `eisopcf` findings, and works at any enclosing declaration — a local
variable, field, method, or class — just like an ordinary Error Prone check. For example,
the common pattern of extracting a value into a local variable and suppressing only that
declaration works:

```java
int m(@Nullable String s, @Nullable String t) {
  @SuppressWarnings("eisopcf")     // suppresses only the finding on this declaration
  int lenS = s.length();
  int lenT = t.length();           // still reported
  return lenS + lenT;
}
```

(Under the hood, the plugin matches at the class and reports findings for the whole
class, so it reconstructs Error Prone's declaration-scoped suppression along each
finding's path; the effect is that `"eisopcf"` behaves like any other Error Prone check
key.)

To turn the whole check off or change its level, use Error Prone configuration:

```
-Xep:eisopcf:OFF
-Xep:eisopcf:ERROR
```

The following table summarizes where each key takes effect:

| `@SuppressWarnings` key | Scope of effect                | Granularity                              |
| ----------------------- | ------------------------------ | ---------------------------------------- |
| `"nullness"`            | Nullness Checker findings      | any (local var, field, method, class, …) |
| `"interning"`           | Interning Checker findings     | any (local var, field, method, class, …) |
| `"allcheckers"`         | all Checker Framework findings | any (local var, field, method, class, …) |
| `"eisopcf"`             | all Checker Framework findings | any (local var, field, method, class, …) |

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

## Runnable example

`docs/examples/eisop-errorprone/` is a small, self-contained Gradle project that runs the
Nullness Checker as the `eisopcf` Error Prone plugin over a source file with a nullness
bug. It consumes the locally-built Checker Framework (so it runs against the current
checkout, before the `framework-errorprone` artifact is published). Run it with:

```
cd docs/examples/eisop-errorprone
make all
```

`make all` builds the required jars, compiles the demo, and checks that the expected
`[eisopcf] [dereference.of.nullable]` diagnostic is produced. It requires JDK 21+ (on
older JDKs it does nothing and succeeds). The project's `build.gradle` is a copyable
template for enabling the plugin in a real Gradle build.

