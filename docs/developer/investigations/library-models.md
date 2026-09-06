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

## Evidence from the NullAway comparison

The benchmark in [errorprone-parity.md](errorprone-parity.md) produced a
by-product this investigation asked for: a list of places where the Checker
Framework rejects code that NullAway accepts, on a real library. Of the 167
findings on Caffeine 3.0.2 that are neither about generics nor about
initialization and that NullAway is silent on, grouping by what is being called:

| call target | count |
| --- | ---: |
| assignment to Caffeine's own unannotated `Pacer` field | 96 |
| `Map.put` | 4 |
| `VarHandle.compareAndSet` / `set` / `setRelease` | 5 |
| `CompletionException(String, Throwable)` | 1 |
| `CompletableFuture.getNow` | 1 |

Only three of these are library-annotation questions at all, and they fall into
three different categories -- which is the useful part.

### Sound: 17 exception classes are missing `@Nullable` on their cause

`new CompletionException("null map", null)` is rejected because
`CompletionException` carries **no annotations at all** in the annotated JDK,
so its `Throwable cause` parameter defaults to `@NonNull`. Its superclass is
annotated correctly -- `Throwable(@Nullable String, @Nullable Throwable)` -- and
`CompletionException`'s constructors do nothing but delegate to it. Its own
javadoc documents the null case: the detail message is
`(cause == null ? null : cause.toString())`.

This is not one class. Auditing `java.base` for exception classes with a
`(… Throwable cause)` constructor:

- **32** such classes
- **15** annotate the cause `@Nullable`
- **17** do not

The 17 include `ReflectiveOperationException`, `NoSuchElementException`,
`ConcurrentModificationException`, `MissingResourceException`,
`RejectedExecutionException`, `IOError` and `CompletionException`. Every one
spot-checked delegates straight to `super(...)` without touching `cause`, so
`@Nullable` there is **sound** -- no null can reach a dereference. The 15 that
are already annotated show the intended form.

This is an inconsistency in the annotated JDK rather than a policy, and it is a
mechanical fix in the `eisop/jdk` fork. It is the clearest sound improvement the
benchmark turned up.

### Unsound convenience: `VarHandle`

`REFRESHES.compareAndSet(this, null, pending)` is correct code: the handle is
for a `ConcurrentMap` field, and null is a legal expected value for a
reference-typed field. But the same call on a handle for a primitive-typed
field throws, and `VarHandle`'s access methods are signature-polymorphic
(`Object... args`), so no single annotation is right for both. That is exactly
what `sometimes-nullable.astub` exists for, and that file says of itself that it
"is very incomplete and should be expanded". `VarHandle` has been added to it,
with a jtreg test; the stub remains opt-in.

### Tempting and wrong: `Map.put`

The four `Map.put` findings look like the same kind of thing and are not. The
code is

```java
Map<Object, Object> result = new LinkedHashMap<>();
result.put(key, null);
```

The Checker Framework is right: a map whose values may be null is
`Map<Object, @Nullable Object>`, and the fix belongs in the calling code. Adding
`Map.put(K, @Nullable V)` to a stub would discard the one mechanism that
expresses this precisely, in exchange for silencing a true positive. Null-value
restrictions on collections are the manual's own example of what belongs in the
conservative annotated JDK, and this is a case of the general rule that a
library-model change is the wrong tool whenever the type system can already say
the thing.

## First experiment

Take Calcite's 48 `.astub` files. For each annotation in them, record: which
library, whether that library now ships JSpecify annotations, and whether the
annotation is a fact about the library or a workaround for a Checker Framework
imprecision. The third category is the interesting one -- it is not a library
problem at all, and it belongs in
[precision-and-adoption-cost.md](precision-and-adoption-cost.md).

[CALCITE-7736]: http://www.mail-archive.com/dev@calcite.apache.org/msg26215.html
