---
name: cf-jtreg
description: Use when adding, changing, debugging, or reviewing a jtreg test in the EISOP Checker Framework — anything under checker/jtreg or framework/jtreg. Triggers on "jtreg", "@requires", "@ignore", a `.out` reference file, "did not meet platform requirements", a test that passes but should not, or gating a test by JDK version. Codifies how jtreg selects and skips tests here, and the traps that make a green run meaningless.
---

# jtreg tests in the EISOP Checker Framework

There are about 130 jtreg test descriptors under `checker/jtreg` and
`framework/jtreg`. jtreg is silent about tests it decides not to run, so most
of the cost in this area is not writing a test but discovering that one never
executed. Read this before trusting a green jtreg run.

`CLAUDE.md` covers **when** to reach for jtreg rather than a JUnit
`CheckerFrameworkPerDirectoryTest`; this skill is about the mechanics once you
have.

## A green run does not mean your test ran

jtreg's summary line reports three ways a test can be skipped, none of which
fails the build:

```
Test results: passed: 122; did not match keywords: 6; did not meet platform requirements: 5
```

- **did not meet platform requirements** — an `@requires` expression was false.
- **did not match keywords** — `@ignore` (the run uses `-keywords:!ignore`).
- A file with no `@test` tag is not a test at all; it is only compiled when
  another test's `@compile` names it.

So confirm what ran, by name, rather than reading the total:

```bash
grep -iE "<yourtest>" checker/build/jtreg/all/report/text/summary.txt
```

**Two traps when reading results:**

- The **work directory accumulates across runs.** `report/text/summary.txt`
  merges them, so two mutually exclusive suites can both show "Passed" — they
  passed under different JDKs, in different runs. `rm -rf checker/build/jtreg`
  before a run whose output you intend to believe.
- Each `.jtr` file is the ground truth for one execution. It records the JDK
  (`testJDK=`), the `@requires` expression it evaluated (`requires=`), and the
  outcome (`execStatus=`). When a test's status is surprising, read its `.jtr`
  rather than re-reasoning about the descriptor.

## `@summary` cannot contain `@Word`

jtreg parses `@Word` in the comment block as a tag. An annotation name in a
summary therefore produces

```
Error. Parse Exception: Invalid tag: AnnotatedFor/@UnannotatedFor
```

Write the name without the `@`: "Writing both AnnotatedFor and UnannotatedFor
…". The existing tests in `checker/jtreg/subpackages` follow this.

## `@ignore` must say why

`@ignore` removes a test from every run, permanently and silently. A bare
`@ignore` has hidden a real failure here for years, and a second one hid a
failure that turned out to be a **wrong expectation in the test**, not a defect
in the code under test.

Always give the reason and a reference:

```java
 * @ignore This fails for Java 11. See typetools issue 2816.
```

When you meet an existing `@ignore`, find out whether it is still true before
working around it. Removing the `@ignore` and reading the failure is usually a
few minutes, and it is how that years-old expectation bug was finally found.

## Gating by JDK version

Two mechanisms, with different scopes:

- **`@requires jdk.version.major >= 25`** in the jtreg comment block, for the
  whole test. Use it when the test needs an API or a language feature from a
  particular JDK. Guards must partition: if one suite says `<= 24` and its
  replacement says `>= 25`, every JDK runs exactly one of them, and a gap or an
  overlap is a silent bug.
- **`@below-javaN-jdk-skip-test`** / **`@above-javaN-jdk-skip-test`** in a JUnit
  test *input* file, honored by `framework-test`'s `TestUtilities`, not by
  jtreg. `framework-test/src/main/java/.../TestUtilities.java` lists the
  versions that exist; a marker for an unlisted version is ignored.

**Gate only what needs gating.** Raising a whole file's floor to skip one
syntax loses coverage on every earlier JDK. Split the version-specific cases
into their own file and gate that; see
`checker/tests/nullness-checkcastelementtype/Issue2050ArrayPatterns.java`,
which holds only the `instanceof` patterns that need JDK 21 while the cast
forms of the same scenarios keep running everywhere.

## Sharing test data between two harnesses

When one set of expectations must run under two different implementations — the
`com.sun.tools.classfile` and `java.lang.classfile` harnesses under
`checker/jtreg/nullness` are the case in point — do **not** copy the test data.
Write the second descriptor as a comment block only, and point its `@compile`
at the first one's data file:

```java
/*
 * @test
 * @summary ... using the java.lang.classfile API.
 *
 * @requires jdk.version.major >= 25
 * @compile ../PersistUtil25.java Driver.java ReferenceInfoUtil.java ../defaultsPersist/Classes.java
 * @run main Driver Classes
 */
```

A file whose own `@test` block makes it a test on one JDK can still be a plain
`@compile` input to another test on a different JDK.

If the data references an API-specific type — an enum constant, say — that is
what forces the copy. Consider making the test annotation hold the constant's
*name* and resolving it with `valueOf` in each harness's driver, which is what
let one 1150-line duplicate collapse to four 12-line descriptors.

## Reference-output tests

`@compile/ref=Name.out` compares the compiler's output to a checked-in file.
Regenerate it by running the same command the descriptor names and capturing
the output, rather than editing it by hand:

```bash
cd checker/jtreg/<dir>
JAVA_HOME=... ../../bin/javac -XDrawDiagnostics -processor <Checker> \
    -d /tmp/out pkg/package-info.java pkg/InPkg.java 2>&1 | tee Name.out
```

The diagnostic position matters: a message reported on an element appears at
that element's declaration line, which is not always where the annotations are.

## Known local red herrings

- `Issue1438`, `Issue1438b`, `Issue1438c` **time out at 20 seconds** when jtreg
  runs concurrently with a JUnit task (`./gradlew test :checker:jtregTests`).
  They pass in a standalone `./gradlew :checker:jtregTests`. Master behaves the
  same; compare before investigating.
- `No java executable at java` means `JAVA_HOME` is unset, not a regression;
  see [`cf-patch-style`](../cf-patch-style/SKILL.md).
- Gradle needs JDK 17+ to run, so select the test JDK with
  `ORG_GRADLE_PROJECT_useJdkVersion=N` rather than by pointing `JAVA_HOME` at an
  older one — and confirm it took effect by reading `testJDK=` in a `.jtr`.
