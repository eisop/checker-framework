# `AnnotatedTypeVariable` primary-qualifier smear: a fix was attempted, and it's confirmed insufficient

**Status: attempted a real fix (non-destructive smear via per-bound provenance tracking),
confirmed by instrumentation that it does not work, root cause narrowed further than
either prior investigation reached. No production code changed; this directory
preserves the repro and the design so the next attempt does not start from zero.**

Investigated 2026-08-13 on branch `fix-atv-primary-bound-smear` (EISOP master
`a1237532e`, i.e. after `#1943`/`#1944`). Background: see
`../cf-tasks8/inferred-types-applier-captured-type-variable.md` in the sibling
`cf-tasks8` directory (the authoritative running record for this defect,
including two earlier follow-up investigations) and
`../cf-tasks9/response-task-2-inferred-types-applier-capture-guard.md`, which
this supersedes with a third follow-up.

## The defect, recap

`DefaultInferredTypesApplier.removePrimaryAnnotationTypeVar`
(`framework/src/main/java/org/checkerframework/framework/type/DefaultInferredTypesApplier.java`)
removes a type variable's primary annotation (dataflow inferred "no change" in
some hierarchy) by re-reading the type variable's *declaration* to reconstruct
the bounds' own qualifiers. That is wrong for a captured type variable: its
bounds come from capture conversion (JLS 5.1.10), not from a declaration, and
its `asElement()` is synthetic (no source, so `getAnnotatedType` on it returns
whatever the checker's defaults produce for "an element from nowhere"). Two
earlier investigations (recorded in `cf-tasks8`'s file above) concluded the
real fix is to make `AnnotatedTypeVariable`'s primary-qualifier smear
(`fixupBoundAnnotations`, which `replaceAnnotations`s the primary onto both
bounds) *non-destructive*, so removing a primary can restore each bound's own
qualifier exactly, without consulting the declaration at all. Both stopped
short of implementing it, calling it a core-invariant change out of scope for
a drive-by patch.

## What was implemented this round

A per-bound, per-hierarchy provenance map on `AnnotatedTypeVariable`:

```java
private @MonotonicNonNull Map<AnnotationMirror, AnnotationMirror> preOverwriteUpperBoundAnnos;
private @MonotonicNonNull Map<AnnotationMirror, AnnotationMirror> preOverwriteLowerBoundAnnos;
```

`fixupBoundAnnotations()` records, for each hierarchy where the primary is
about to overwrite a bound's own *different* qualifier, that qualifier --
keyed by the hierarchy's top, and only the first divergence since the last
restore (`Map.putIfAbsent`), so a second overwrite doesn't clobber the record
with an already-smeared value. A hierarchy where the bound's own qualifier
already agrees with the primary is not recorded, since smearing it is then a
no-op.

A new method, `removePrimaryAnnotationRestoringBounds(AnnotationMirror top)`,
removes the primary in that hierarchy (via the existing, unchanged
`removeAnnotationInHierarchy`, which still recursively strips the bound's
copy too) and then restores each bound to the recorded pre-overwrite value if
there is one, or to whatever the bound had immediately before this call if
there isn't (the "nothing was smeared" case, which restores the bound to
itself, a no-op). `DefaultInferredTypesApplier.removePrimaryAnnotationTypeVar`
was rewritten to call this instead of the declaration-reconstruction dance,
dropping the subtyping-check gate entirely: restoring a value this exact type
variable itself once legitimately had is sound by construction, unlike
guessing from a possibly-wrong declaration.

This is deliberately scoped to *not* survive `deepCopy`/`shallowCopy`/
`AnnotatedTypeCopier` -- the provenance maps are not propagated by the
copiers. That was a considered simplification, justified by tracing the
actual call path: `GenericAnnotatedTypeFactory.addComputedTypeAnnotations`
applies defaults (including whatever first smears a primary onto the bounds)
and then, in the same call, on the same object, applies inferred types (the
removal) -- no copy happens in between for the reported bug's own path. A
copy made between an overwrite and a removal elsewhere in the framework would
just fall back to "no record, leave the bound as the copy inherited it,"
which is a missed restoration, not a new corruption.

## Why it still doesn't work

Instrumented `recordPreOverwriteAnnos` and `removePrimaryAnnotationTypeVar`
(temporary `System.err` prints gated on `ATV_DEBUG`, not committed) and ran
`CapRepro2.java`'s `refined` method through `checker/bin/javac -processor
nullness`:

```java
<T extends @Nullable Object> void refined(List<? extends T> l) {
    var x = l.get(0);
    if (x != null) {
        x.toString();   // fix => method.invocation.invalid:
    }                   //   found: capture#02 extends T extends @UnknownInitialization Object
}                       //   required: @Initialized Object
```

Baseline (unmodified master): 0 errors, matching the earlier investigations'
finding that the corruption is silent for built-in checkers. With this
round's fix: 1 error, in the *Initialization* hierarchy, not the one the
`var`/capture shape is usually reasoned about (Nullness).

Tracing `recordPreOverwriteAnnos`'s own log line at the exact moment of first
divergence:

```
recordPreOverwrite: top=UnknownInitialization current=null newAnno=UnknownInitialization bound=Object
recordPreOverwrite: top=UnknownInitialization current=null newAnno=UnknownInitialization bound=NullType
```

`current=null`. The capture's upper bound (`Object`) had *no* qualifier yet
in the Initialization hierarchy at the instant its primary (`@Initialized`'s
sibling `@UnknownInitialization`, `@DefaultFor(LOCAL_VARIABLE)` per
`checker-qual/.../initialization/qual/UnknownInitialization.java`) was first
smeared onto it. So there was nothing to record. The bound's *own* correct
default (`@Initialized`, `@DefaultFor(IMPLICIT_UPPER_BOUND)` per
`checker-qual/.../initialization/qual/Initialized.java` -- i.e. what an
unannotated `<T extends Object>`'s bound should default to) never gets
applied: `fixupBoundAnnotations`'s `replaceAnnotations` fills the hierarchy
first, and by the time any later defaulting pass would visit the bound, it
already sees an annotation there and -- correctly, by its own contract --
does not overwrite an already-present one.

This is a **race between bound laziness and primary defaulting**, not (only)
"the declaration is the wrong source," which is what both earlier
investigations diagnosed. `AnnotatedTypeVariable.getUpperBound()` /
`getLowerBound()` lazily materialize a bound on first access
(`BoundsInitializer.initializeBounds`), and `fixupBoundAnnotations` runs
`replaceAnnotations` from that same lazy-init path *and* from
`addAnnotation`/`setUpperBound`/`setLowerBound`. If a primary gets set on the
type variable (e.g. the `LOCAL_VARIABLE` default) before the bound has ever
been lazily realized, the bound's very first observed state already has the
smear baked in -- there is no earlier "clean" moment for anything, including
this round's provenance map, to have captured. This confirms, with a
concrete mechanism instead of a general "no authoritative source" statement,
exactly what the two prior investigations already concluded from a different
angle (the declaration is unreliable): **the pre-smear value does not
reliably exist to be preserved by any fix that only touches
`fixupBoundAnnotations`/the applier.** A real fix needs the bound's own
default to be resolved before any primary can ever be smeared onto it --
i.e., changing how `QualifierDefaults`/`BoundsInitializer` interact with
`AnnotatedTypeVariable`'s laziness, not just how the smear is undone. That is
a bigger, genuinely core-invariant change, matching what
`cf-tasks8`'s file already called "out of scope for a drive-by patch," now
with a specific mechanism to aim at instead of a general description.

## Artifacts in this directory

- `CapRepro2.java`: the three shapes tested (`viaVar`, `refined`,
  `catListAndIterable`, the last matching `WildcardIterable.java`'s
  enhanced-for-over-`Iterable<? extends T>` shape from
  `checker/tests/all-systems`). All three are 0 errors on unmodified master.
  `refined` alone is sufficient to reproduce this round's finding once the
  attempted fix (not included here; see the code sketch above, which is
  complete enough to reconstruct) is applied.

## Suggested next step

Before attempting another patch to `fixupBoundAnnotations`/the applier
directly, first determine whether `AnnotatedTypeVariable`'s bounds can be
required to go through their own defaulting *before* the type variable's own
primary is ever allowed to be set (e.g., forcing `getUpperBound()`/
`getLowerBound()` realization earlier in `QualifierDefaults`'s pipeline, or
having `annotate(tree, type)` visit a type variable's bounds before its
primary). That is a different, and probably larger, change than anything
tried in either this or the two earlier rounds; it was not attempted here due
to time and risk (it touches the general defaulting pipeline, not just
`AnnotatedTypeVariable`), and needs its own investigation into whether
reordering is even safe for every other caller of `getUpperBound`/
`getLowerBound` and `QualifierDefaults`.
