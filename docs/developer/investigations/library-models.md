# Who should maintain the nullness of other people's code?

## The problem

To check a program, the Checker Framework needs to know the nullness of every
library that program calls. It gets that from annotation files: `.astub` files,
and the annotated JDK. Somebody has to write them.

Apache Calcite's proposal to leave the Checker Framework ([CALCITE-7736]) names
this second among its reasons: the setup carries "48 `.astub` files that patch
the nullness of the JDK and of third-party libraries." NullAway, it observes,
"needs no separate plugin and no stub files: it ships nullness models for the
JDK and for popular libraries."

That is a per-project tax that scales with the number of libraries a project
uses, paid by every project independently, for the same libraries. The
typetools tracker shows the same shape from the other side: `awt package not
annotated` (#1188) has run since 2017, `Is there a way to add aliases for
third-party annotations?` (#3625), `Standard astub file for unsound treatment
of reflection APIs?` (#3258).

This repository ships 84 `.astub` files. That is the supply. The 48 in one
downstream project is the unmet demand.

## What is already true

Two things make this more tractable here than it looks.

The eisop fork already reads **binary stubs**, so the cost of shipping a large
body of annotations is no longer parse time on every compile. Whatever the
distribution mechanism turns out to be, the reading end exists.

The Checker Framework can already *infer* annotations from a library's own
source: whole-program inference (`-Ainfer`) exists and is documented at length.
Nobody appears to have pointed it at the top 100 Maven artifacts and published
the result. The tool that produces library models may already be written.

The ecosystem has also changed the target. Spring Framework 7 and Spring Boot 4
(November 2025) annotate their whole portfolio with JSpecify. Libraries are
starting to ship their own nullness, which is the outcome that makes this
problem disappear -- for the libraries that do it. The question is what happens
to the rest.

## The three questions

1. **What fraction of the annotations a real project needs are about libraries
   that now ship their own JSpecify annotations?** If the answer is most of
   them, this problem is solving itself and the work is to read JSpecify
   annotations faithfully -- a correctness problem, not a content problem. If it
   is a minority, the content has to come from somewhere. Calcite's 48 stub
   files are a ready-made sample: classify what each one is patching.

2. **Can whole-program inference produce a usable model for a library it was
   not designed for, without that library's test suite?** WPI's documented
   weakness is that "inference results depend on uses in your program or test
   suite." A library analyzed on its own has no uses. Whether the output is
   worth shipping -- and whether it is *sound* to ship, given that inferred
   annotations are not verified -- decides whether models can be generated or
   must be written.

3. **What would it cost to be wrong?** A shipped model that says a method
   returns `@NonNull` when it can return null converts the Checker Framework's
   central promise into a false negative, silently, for every user of that
   model. NullAway accepts this tradeoff. Whether the Checker Framework can is
   a question about what it claims to be, and it should be answered before any
   model ships, not after.

## First experiment

Take Calcite's 48 `.astub` files. For each annotation in them, record: which
library, whether that library now ships JSpecify annotations, and whether the
annotation is a fact about the library or a workaround for a Checker Framework
imprecision. The third category is the interesting one -- it is not a library
problem at all, and it belongs in
[precision-and-adoption-cost.md](precision-and-adoption-cost.md).

[CALCITE-7736]: http://www.mail-archive.com/dev@calcite.apache.org/msg26215.html
