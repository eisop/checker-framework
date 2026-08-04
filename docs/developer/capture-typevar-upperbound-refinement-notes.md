# Capture type-variable upper-bound refinement notes

This is an investigation log, in the spirit of
[`performance-notes.md`](performance-notes.md): it records what was
verified, what was only relayed from another repository, and what remains
open, so the analysis does not have to be redone from scratch.

Findings are tagged **[verified]** (reproduced here on this commit),
**[relayed]** (taken from the reference-checker brief / GitHub issue, not
independently reproduced in this worktree), or **[open]**.

## Context

The `jspecify-reference-checker` reports false positives on
`CaptureConvertedTypeVariableBounded.java:97,102` (Strict also `:72`). The
symptom was root-caused in that repo to
`NullSpecAnnotatedTypeFactory.getUpperBounds`, which — when its
null-establishing-path walk reaches a **non-captured** `AnnotatedTypeVariable`
— discards the instance's upper bound and re-derives it from the
type-parameter *declaration* element:

```java
if (type instanceof AnnotatedTypeVariable
        && !isCapturedTypeVariable(type.getUnderlyingType())) {
    AnnotatedTypeVariable variable = (AnnotatedTypeVariable) type;
    typeParameterElement = variable.getUnderlyingType().asElement();
    type = getAnnotatedType(typeParameterElement); // re-derive from declaration
}
```

Its own comment attributes the re-derivation to
[eisop/checker-framework#737](https://github.com/eisop/checker-framework/issues/737).
The question this note answers: **can CF be changed so that a checker like the
reference-checker no longer needs this declaration re-derivation** — i.e. can
the refined instance bound be made reliably trustworthy?

## What CF gets right (the standard-lattice baseline) — [verified]

`framework/tests/typedeclbounds/CaptureTypeVarBoundRefine.java` maps the
construct onto the `typedeclbounds` lattice (`@Bottom <: @S1 <: @Top`). It
passes on `master` (`ab0706329`): capture of `Inner<? extends @Bottom Object>`
against `class Inner<U extends T>` yields a capture whose upper bound is the
type variable `T` with `T`'s inner upper bound refined to `@Bottom`, and
`p.get()` reads as `@Bottom` (no spurious error); the `? extends @Top Object`
contrast correctly errors. So on a standard, reflexive lattice CF computes and
*retains* the refined capture bound end to end. The CF API that exposes it is
just `AnnotatedTypeVariable.getUpperBound()` on the capture's bound type
variable — it already returns the refined instance bound. **No new CF API is
required to read the refinement; it already exists.**

The refinement is produced in `AnnotatedTypeFactory.annotateCapturedTypeVar`:
the capture's upper bound is `annotatedGLB(substituted declared UB, wildcard
extends bound)`, which for `U extends T` and `? extends @Bottom Object` is `T`
carrying the tightened inner bound. That value is correct and present on the
instance.

## Is #737 the same mechanism as the `getUpperBounds` re-derivation? — [verified]: no, different (in fact opposite)

Issue #737 was reproduced on this commit (`ab0706329`) with the issue's own
example (`class Sub extends Min<XXX>.Super`, `XXX extends @Nullable Object`) and
a print in `SupertypeFinder.supertypesFromTree`:

```
type=@NonNull Min<XXX extends @Nullable Object>.@NonNull Sub
  supertypes=[@NonNull Min<XXX extends @NonNull Object>.@NonNull Super]
```

`Sub` correctly carries `XXX extends @Nullable Object`, but its computed
supertype's enclosing `Min` shows `XXX extends @NonNull Object`: the type
variable's **declared** bound (`@Nullable`) has been replaced by the lattice
**default top** (`@NonNull`) while computing the supertype through an enclosing
parameterized type. No capture conversion, no wildcard, no GLB is involved. The
cited `TypeFromTypeTreeVisitor.visitMemberSelect` workaround from the issue is
**not** present in current code (`visitMemberSelect` handles only the `TYPEVAR`
case), so #737 is still open.

The two defects are distinct, and their error directions are **opposite**:

| | #737 (enclosing-type supertype) | `getUpperBounds` re-derivation (capture) |
|---|---|---|
| Trigger | supertype of nested class via enclosing parameterized type | capture conversion of a bounded wildcard |
| Instance bound | **wrong — too strong** (`@NonNull` default instead of declared `@Nullable`) | **correct — refined** (`@Bottom`/`@MinusNull` from the wildcard) |
| Effect of re-deriving from the declaration element | **fixes** it (declaration has `@Nullable`) | **breaks** it (declaration has `@Nullable`/`@Top`, discarding the refinement) |

A single "always re-derive from the declaration" step cannot satisfy both: it
corrects #737's over-strong instance bound and simultaneously clobbers the
capture case's correctly-refined instance bound. So the reference-checker
comment cites #737 as *the general class of "instance bound may be
untrustworthy" problem the re-derivation guards against*, not as a precise
mechanism match. The capture false positive is the re-derivation
**overcorrecting** into a case where the instance bound was already right.

## Design: what a clean CF-side solution would require — [analysis]

Because the instance-reading API already exists, the only thing that would let
the reference-checker drop its re-derivation and simply trust
`getUpperBound()` is for the **instance bound to be reliable** — i.e. for
**#737 to be fixed** so supertype computation through an enclosing
parameterized type preserves each type variable's declared bound qualifier
rather than defaulting it. There is no intermediate "expose X" that helps: any
CF helper that returned a *corrected* bound would itself have to detect the
#737 corruption, which is exactly the unsolved problem.

So the honest shape of a "nice CF solution" seemed to be two-part and CF-led:

1. **CF:** fix #737 — make `SupertypeFinder` / the enclosing-type annotation
   path carry the type parameter's declared bound into the computed supertype's
   type arguments (the maintainer's sketch in the issue clears and re-copies the
   enclosing type in `visitMemberSelect`).
2. **Reference-checker:** then delete the `getAnnotatedType(typeParameterElement)`
   re-derivation in `getUpperBounds` and use the instance
   `AnnotatedTypeVariable.getUpperBound()`, which now carries both the correct
   declared bound (from #737 being fixed) and the capture refinement.

**This two-part shape was tested directly after the #737 fix below was
implemented, and does not hold** — see "Does the #737 fix unblock the
reference-checker?" at the end of this document. Left here for the historical
reasoning trail; do not treat it as the conclusion.

## Why no CF change is attempted here — [decision]

Fixing #737 is high blast radius and is a known-hard problem:

- It lives in `SupertypeFinder`, core supertype computation exercised by every
  checker.
- The issue author (the maintainer) already tried the `visitMemberSelect`
  patch and found it **insufficient**: the equivalent identifier form
  (`class Sub extends Super`) is "harder to come by," the extends/implements
  validation in `BaseTypeVisitor` is also affected, and the note ends "Maybe
  `TypeFromTypeTreeVisitor` is the wrong place to fix this issue." So even the
  localized sketch is not known-correct.
- The reference-checker brief documents that prior attempts in this area
  regressed unrelated samples.

A speculative partial fix here would violate "verify before claiming" and
"don't add speculative extension points." The instance-reading API the design
depends on (`AnnotatedTypeVariable.getUpperBound()`) already exists, so there is
nothing to add pre-emptively. Conclusion: **no low-risk pure-CF change is
available in this pass.** The correct next step is a dedicated #737 fix,
validated against `./gradlew alltests` plus the reference-checker sample suite
in both Lenient and Strict, at which point the reference-checker's
re-derivation can be retired.

## Lock-in

`framework/tests/typedeclbounds/CaptureTypeVarBoundRefine.java` is the pure-CF
regression lock-in: it demonstrates CF retains the capture refinement on a
standard lattice and must keep passing. If a future #737 fix touches capture or
supertype bound propagation, this test guards the standard-lattice behavior.

## #737 fix attempt — findings — [verified]

A follow-up pass attempted the #737 fix directly. Reproduced on the current
branch with the issue's own `Min`/`Sub` example and a print in
`SupertypeFinder.supertypesFromTree`:

- Both the **qualified** form (`class Sub extends Min<XXX>.Super`) and the
  **identifier** form (`class Sub extends Super`) produce the same pre-
  substitution corruption: the computed supertype's enclosing `Min` shows
  `XXX extends @NonNull Object` (defaulted top) instead of the declared
  `XXX extends @Nullable Object`.
- **The final supertype returned by `directSupertypes` is already correct for
  both forms** — the `TypeVariableSubstitutor` step in
  `SupertypeFinder.visitDeclared` maps `XXX` to the instance's (correct) type
  argument and overwrites the enclosing bound. The corruption is purely in the
  intermediate pre-substitution value.
- The only place the raw (non-substituted) tree-derived type leaks is
  `AnnotatedTypeFactory.getTypeOfExtendsImplements` (used by
  `BaseTypeVisitor.processClassTree` to validate extends/implements clauses).

### The extends/implements "invalid not rejected" concern is a *separate* gap

The issue's worry that an invalid supertype clause
(`class Sub extends Outer<@NonNull String>.Super` where the argument violates
`XXX`'s bound) is not rejected is **not caused by the #737 bound corruption**.
`BaseTypeValidator.visitDeclared` scans only the *top-level* type's direct type
arguments; it never scans an enclosing type's type arguments (verified: an
out-of-bound enclosing argument is not rejected in extends **or** field
position, whereas the same argument on a direct, non-enclosing parameterized
type *is* rejected). Fixing the bound corruption therefore does not — and did
not — change that behavior. Rejecting invalid enclosing-type arguments would
require extending `BaseTypeValidator` to recurse into enclosing types, which is
an independent change.

### Fix that was applied

Rather than the issue's `visitMemberSelect` sketch (which re-derives the
enclosing type from `tree.getExpression()` and so cannot handle the identifier
form, which has no such expression), a single helper
`TypeFromTypeTreeVisitor.refineEnclosingTypeVariableBounds` refines, for any
tree-derived declared type, the bounds of type-variable arguments in its
enclosing types via the existing `getTypeVariableFromDeclaration`. It is called
from both `visitIdentifier` and `visitMemberSelect`, so it fixes **both** forms
uniformly. Concrete (non-type-variable) enclosing arguments are left untouched
(they are already correct), so the qualified form's `Outer<@Nullable String>.…`
case is unaffected.

Verified: the intermediate supertype is now faithful for both forms;
`./gradlew :framework:test` and `./gradlew :checker:test` pass with zero
failures and **zero diagnostic changes** across the whole corpus.

### Caveat — no behavioral regression test is possible

Because the final supertype was already correct (substitution) and the
extends/implements validator never observes enclosing-type arguments, the fix
has **no observable diagnostic effect on the built-in lattices** — which is
exactly why the full test corpus is unchanged, and why no `// :: error:`-style
regression test can capture it. The fix is a fidelity improvement whose
beneficiary is a type system with stricter substitution (the
jspecify-reference-checker `getUpperBounds` re-derivation this note began with);
that repo's samples are the meaningful end-to-end check and were not run here.

## Does the #737 fix unblock the reference-checker? — [verified]: no

The design section above assumed that once #737 is fixed,
`getAnnotatedType(typeParameterElement)` — the call `getUpperBounds` uses to
re-derive a non-captured type variable's bound from its declaration — would
return the refined instance bound instead of the plain declared one, letting
the reference-checker delete that re-derivation. This was tested directly,
after the #737 fix, and does **not** hold.

Instrumented `AnnotatedTypeFactory.annotateCapturedTypeVar` (temporary print,
reverted) to call `getAnnotatedType(elt)` on the capture's upper-bound type
variable's element — exactly what `getUpperBounds` does — and compared it to
the instance bound, on the existing `Outer<T>`/`Inner<U extends T>` lock-in
test, **with the #737 fix applied**:

```
capture upperBound(instance)=T extends @Bottom Object
rederived-getAnnotatedType(elt)=T extends @Top Object
```

`getAnnotatedType(elt)` still returns `T`'s plain declared bound (`@Top`), not
the refined instance bound (`@Bottom`) — unchanged by the #737 fix. This is, in
fact, correct behavior for that call: `getAnnotatedType(TypeParameterElement)`
answers "what is `T` declared as," and `T` is declared `extends @Top Object`
regardless of any particular capture. It was never wrong, and #737 never
touched it: #737's corruption is specifically in computing a **nested class's
supertype through an enclosing parameterized type** (`SupertypeFinder`
resolving an `extends`/`implements` clause via `TypeFromTypeTreeVisitor`), an
entirely different operation from an element-based declaration lookup on a
type parameter.

**Conclusion:** the #737 fix above is a real, independently valid fix — it
corrects genuine corruption in enclosing-type supertype computation, verified
on both the qualified and identifier forms, with zero regressions. But it does
**not** provide a path to retiring the reference-checker's `getUpperBounds`
re-derivation, because that re-derivation's actual problem (discarding a
capture's correctly refined instance bound in favor of the type variable's own
never-corrupted declaration) is not something #737 causes or #737's fix
touches. The "two-part solution" hypothesized above is disproven; resolving
the reference-checker's capture false positive needs a different, more
targeted discriminator — most plausibly one that recognizes when the type
variable being re-derived is itself the upper bound of a captured type
variable, so the instance bound should be trusted instead of re-derived — and
that discriminator lives in the reference-checker, not in CF.
