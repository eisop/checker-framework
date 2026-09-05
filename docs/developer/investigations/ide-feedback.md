# The Checker Framework has no editor

## The problem

There is no IDE integration. The manual is direct about it: "There is no
dedicated Eclipse plug-in for running the Checker Framework" (external-tools,
§Eclipse), and the IntelliJ IDEA section tells the reader to configure javac to
run the processor on every compile -- and, if the project uses a build tool, not
to do even that but to run the build tool instead.

So the shortest feedback loop the Checker Framework offers is a full compile.
There is no squiggle under the expression, no hover explaining why a type is
`@Nullable`, no quick-fix, and no way to see the effect of adding an annotation
without rebuilding.

Meanwhile the alternative acquired all of that. IntelliJ IDEA 2025.3 aligned
its nullness user experience with NullAway and now prefers JSpecify annotations
over JetBrains' own when both are on the classpath. A developer choosing a
nullness tool in 2026 is choosing between one that draws in the editor and one
that does not.

This is the gap that is least about the analysis and most about whether people
use it.

## What is already true

The pieces a language server would need mostly exist, in a form nobody has
assembled:

- `DiagnosticSink` (PR #1993) already decouples findings from javac's
  `Messager`. A host that is not a compiler can receive them.
- `SuggestedFixData` (also #1993) already carries machine-applicable fixes
  through that seam, in source offsets, with no dependency on any host
  framework. Quick-fixes have a representation.
- The Error Prone plugin proves a host can drive the checker over an AST it did
  not create -- which is what an editor does.

What does not exist is anything incremental. Every part of the framework
assumes a whole compilation unit, `setRoot` clears caches per unit, and the
dataflow analysis runs per method body from scratch. An editor needs an answer
for the method being typed, in the time between keystrokes.

## The three questions

1. **How long does the Checker Framework take to answer a question about one
   method?** Not one file, one method. If a warm process can re-check a single
   body in tens of milliseconds, an editor integration is a plumbing project.
   If it takes a second because the type factory has to be rebuilt, the answer
   is architectural and much larger. This is measurable today with the existing
   `typeProcessExternally` entry point, and it should be measured before
   anything is designed.

2. **What is the smallest thing that would be useful?** A full language server
   is not the only option, and probably not the first one. Publishing findings
   in SARIF (typetools#5666 asks for a report file; typetools PR #546 drafts
   SARIF output) would put them in every IDE and code-review tool that reads
   SARIF, with no editor plugin at all. That is a different, much cheaper
   product, and it may capture most of the value.

3. **Does an editor need the whole checker?** IntelliJ already computes
   nullness; what it lacks is the Checker Framework's answer. A plugin that
   surfaced findings from the *build* -- rather than recomputing them -- would
   need no incremental analysis at all, only a way to map a stored finding to a
   current source position. Whether that is enough to change anyone's mind is
   the real question, and it is a user question, not a technical one.

## First experiment

Instrument `typeProcessExternally` to report the wall time of one class, in a
warm process, over a range of class sizes. That single number decides which of
the three questions above is worth pursuing first.

There is already a hint. A scaling measurement recorded in
[errorprone-parity.md](errorprone-parity.md) found that on annotated code the
checker's cost over plain `javac` stays in a 1-2s band whether it compiles 5
files or 275 -- it looks close to a fixed startup cost rather than a per-file
one. An editor is the one host that can amortize a fixed cost: it pays it once
per session, not once per keystroke. If that holds up on unannotated code, the
per-method number this experiment measures could be small enough to matter,
and question 1 becomes the promising one rather than the disqualifying one.
