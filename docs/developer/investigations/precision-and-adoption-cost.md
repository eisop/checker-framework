# What does it cost to make an existing codebase pass?

## The problem

The largest single category on the typetools issue tracker is imprecision.
Of 396 open issues, **47** are labelled *False Positive (false warning or
imprecision)* -- against 36 bugs and 27 false negatives. Imprecision outnumbers
unsoundness reports by nearly two to one.

That ratio is the adoption cost. A false positive is not a wrong answer the
user can ignore; it is an annotation they must write, a suppression they must
justify, or a refactoring they must perform, on code that was already correct.
Multiply by a codebase and the question "should we adopt this?" becomes a
budget question.

It is a large enough problem to have attracted its own research: *LLM-Based
Repair of Static Nullability Errors* (Karimipour et al., arXiv 2507.20674)
exists because "the sheer volume of violations makes manual correction
impractical for large codebases." That paper targets NullAway. The Checker
Framework finds strictly more, so it has strictly more of this problem.

## What is already true

The Checker Framework's own answer is whole-program inference: run the checker
in inference mode, have it write the annotations, iterate to a fixed point.
`inference.tex` documents it thoroughly, including its limits -- inference
results "depend on uses in your program or test suite," it "ignores some code,"
and its output needs manual checking.

But WPI is not in the product. It needs an external driver (`do-like-javac`),
its own scripts, and a batch workflow measured in project-wide runs. A
developer who has just added the checker to their build and sees 800 errors
cannot reach for it in the next five minutes.

There is also no baseline. Nobody can say how many annotations a typical
100-KLOC Java project needs, how many of those WPI can infer, or how the number
compares to NullAway on the same code. Without that number, "adoption is
expensive" is a feeling, and every proposal to reduce it is unfalsifiable.

## The three questions

1. **What is the annotation cost, per KLOC, of making a real project pass?**
   Count annotations added, suppressions added, and code changed, on a project
   that has done this -- Calcite, Guava, and this repository's own downstream
   corpus have all paid it. Split the suppressions by cause: a genuine
   unsoundness the developer accepts, versus an imprecision the checker should
   not have reported. The second number is the addressable one, and it is the
   number that says whether to work on precision or on tooling.

2. **Of the imprecisions that cost real projects the most, how many share a
   cause?** 47 open issues is a lot of individual reports and possibly a small
   number of underlying gaps. If ten of them are one weakness in how
   generics interact with flow refinement, that is a project. If they are 47
   unrelated cases, precision work is a treadmill and the effort belongs in
   tooling that makes suppressions cheap instead.

3. **Can inference close the gap, and how would we know it was safe?** WPI can
   already write many of these annotations. It is not verified: an inferred
   annotation is a guess that happens to typecheck. Running WPI, then checking
   the result, then measuring how many findings survive would answer both parts
   at once -- how much it removes, and what it costs in soundness.

## First experiment

Take one downstream project already in CI (`plume-lib` is small and annotated;
Guava is large and annotated). Reconstruct, from its history, every annotation
and suppression added to make the Checker Framework pass. Classify each one.
Publish the per-KLOC number. Everything above depends on it.
