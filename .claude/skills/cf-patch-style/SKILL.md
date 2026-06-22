---
name: cf-patch-style
description: Use when authoring commits, patches, or pull requests for the EISOP Checker Framework. Triggers on requests to write a commit message, produce a git format-patch, prepare a PR, or stage changes for review. Codifies the project's commit conventions, branch naming, changelog protocol, and what not to touch.
---

# Patch and commit style for the EISOP Checker Framework

The maintainer's strong preference is for small, focused, commit-ready
patches with informative commit messages and minimal prose around them.
Follow this skill when producing any change for review.

## One logical change per commit

Series of three or four narrow commits are preferred over a single
sprawling diff. Each commit should be reviewable on its own and revertable
without disturbing the others. Refactoring, performance, and behavioral
fixes belong in separate commits even when they touch the same file.

## Commit subject line

Imperative mood, scope first when it adds clarity, concise. Examples
copied verbatim from real history:

- `Avoid Integer boxing and lambda for ATM hashCode`
- `Clear SubtypeVisitHistory to avoid huge caches that slow down checking`
- `Cache the hashCode for dataflow expressions`
- `Increase default cache size from 300 to 2000`
- `Use cached value of Class::getCanonicalName`
- `Optimize CFAbstractValue#validateSet and avoid allocation`
- `Review of common/basetype package` (for systematic audits)

Avoid: "Improve performance", "Fix bug", "Various changes", "WIP".

## Commit body

Two short paragraphs is usually enough. Structure:

1. **Problem.** What was wrong, and how was it observed. Cite the JFR
   self-time percentage or test failure if applicable.
2. **Fix.** What the change does. Mention any non-obvious invariant
   preserved or trade-off accepted.

Example:

```
AnnotatedTypeScanner.reset: clear visitedNodes instead of reallocating

Every call to AnnotatedTypeMirror.hashCode()/equals() goes through the
static HASHCODE_VISITOR / EQUALITY_COMPARER, whose reset() allocated a
fresh IdentityHashMap. JFR alltests traces attribute roughly 8.6% of
main-thread self-time to this allocation path.

IdentityHashMap.clear() nulls the table in place with no rehash or
realloc, so for a steady-state visitor it is strictly cheaper. Update
the comment that cited an older measurement favoring reallocation.
```

Do not include marketing adjectives ("blazingly", "dramatically",
"massively"). Numbers are more persuasive than prose.

## Commit trailers

- Keep the `Co-Authored-By:` trailer.
- **Never add a `Claude-Session:` trailer.** Strip it from any commit
  message before committing, even if the harness boilerplate suggests
  it. If it slipped into commits that have not been pushed, rewrite
  them to remove it (e.g. `git filter-branch --msg-filter
  "grep -v '^Claude-Session:'"` over the range).

## Branch naming

- Performance/correctness audits of a package: `review-<package>` or
  `perf-<package>`, e.g., `review-common-basetype`.
- Bug fix: `fix-<short-description>`.
- Feature: `feature-<short-description>`.
- Infrastructure: descriptive, e.g., `gradle-9.4.1`.

## Changelog

Every user-visible or perf-relevant change adds a bullet to the next
release section in [`docs/CHANGELOG.md`](../../../docs/CHANGELOG.md).
Match the existing style: one line, ends with the PR number once it's
opened.

## What not to touch in a perf patch

- **`checker-qual/`** is public API. Signature changes break downstream.
- **Default values in public classes** without a release note.
- **`AnnotatedTypeMirror` equality/hash contract.** Cache it, don't
  redefine it.
- **`@Pure`/`@Deterministic`/`@SideEffectFree` annotations on methods**
  unless you understand the dataflow consequences.

## When producing a patch series via `git format-patch`

Generate to a clean directory:

```
git format-patch origin/master -o /tmp/cf-patches/
```

Each file is standalone and applies with `git am`. Verify round-trip
before submitting:

```
git checkout -b verify origin/master
git am /tmp/cf-patches/*.patch
./gradlew assemble
git checkout - && git branch -D verify
```

## Test requirements

- `./gradlew assemble` — must succeed.
- The relevant focused test(s) — e.g., `:checker:NullnessTest`.
- `./gradlew alltests` — strongly preferred for any framework or
  javacutil change. Subtle visitor and dataflow semantics often fail
  only in obscure checkers.
- Run `./gradlew spotlessApply` before committing to ensure all formatting is
  correct.

If `alltests` is impractical (e.g., no local JDK matrix), say so
explicitly in the PR description rather than implying it passed.

## What good output to a human reviewer looks like

- A branch with N small commits, each compiling.
- A PR description that summarizes the series in two or three sentences.
- A `docs/CHANGELOG.md` entry per user-visible change.
- No drive-by formatting churn, no `import` reordering on untouched
  files, no IDE-config commits.
